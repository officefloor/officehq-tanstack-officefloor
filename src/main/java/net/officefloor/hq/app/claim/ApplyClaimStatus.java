package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim its number (see {@link ClaimNumbers}); a voided claim keeps it (and it is never reused).
 *  Settling fixes the claim's due at the figure it has then: the one its person's last statement
 *  bound it to, else its person's grade's rate in force on the claim's date, plus its late charge then, at least the sheet's floor; with no rate it stays live. The user who opened a claim, or last edited it, may not settle it.
 *  When a clerk settles a claim whose due is over the sheet's clerk limit (a due of n/a is within any
 *  limit) it goes PENDING instead, its due not yet fixed (still bound by a statement that binds it); a steward settles anything. Only a steward
 *  approves a PENDING claim (it settles, its due fixed at its figure then, bound or live, plus its late charge, at least the floor),
 *  and never the user who opened it or last edited it; a rejection sends it back to OPEN, still bound by the last
 *  statement if that bound it.
 *  A claim whose due is above the proof threshold in force on its date (its person's grade's then when
 *  one is set, else the sheet's) cannot be settled or
 *  approved while it has no proofs, nor while it lacks a proof of each of the sheet's proof kinds (refused before the clerk limit is considered).
 *  A claim whose due is above the sheet's note threshold in force on its date cannot be opened while
 *  its note is empty.
 *  A claim whose due is above the sheet's review threshold in force on its date goes to REVIEW when
 *  opened instead of OPEN: it records who opened it but has no number yet. Only a steward reviews it,
 *  which opens it (it gets its number then).
 *  A deferred claim cannot be settled until the sheet's today reaches its deferred-until date.
 *  Only a steward may void a SETTLED claim, and only while the sheet's today is within 7 days of its
 *  settlement (its status log's SETTLED date); its frozen due then leaves every total.
 *  A claim due by in a period with a cap cannot be opened while its person already has that many
 *  OPEN or PENDING claims due by in it.
 *  While its person's flag is in force only a steward may open a claim.
 *  A claim due by in a closed period cannot change status at all. Opening, settling and voiding are
 *  only for the roles the sheet's permission matrix allows (see {@link Settings#allows}).
 *  While the sheet's list of void reasons is not empty, a void needs a reason from it, which the
 *  claim keeps.
 *  A DRAFT claim can be deleted; a DELETED one restored to DRAFT only on the sheet's today it was
 *  deleted (its status log's last DELETED date). */
public class ApplyClaimStatus {
    /** How many days after its settlement (the status log's SETTLED date) a steward may still void a claim. */
    static final int SETTLED_VOID_DAYS = 7;

    public void apply(@Val ClaimStatusChange change, @Val Claim claim, ClaimNumbers numbers, People people,
            Figures figures, ClosedPeriods closedPeriods, Settings settings, ClaimLog log, Sheets sheets,
            VoidReasons voidReasons)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        String perm = change.action() == ClaimAction.OPEN ? "open" : change.action() == ClaimAction.SETTLE ? "settle"
                : change.action() == ClaimAction.VOID ? "void" : null;
        if (perm != null && !settings.allows(claim.getSheetId(), perm, change.user())) {
            throw new ClaimStatusRefusedException("Your role may not " + perm + " a claim");
        }
        if (change.action() == ClaimAction.OPEN && settings.role(claim.getSheetId(), change.user()) != Role.STEWARD
                && people.find(claim.getSheetId(), claim.getPersonId()).isFlagged()) {
            throw new ClaimStatusRefusedException("Only a steward can open a claim of a flagged person");
        }
        if (change.action() == ClaimAction.OPEN && (claim.getNote() == null || claim.getNote().isBlank())) {
            BigDecimal threshold = settings.noteThreshold(claim.getSheetId(), claim.getDate());
            if (threshold != null) {
                String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
                BigDecimal due = figures.due(claim, grade);
                if (due != null && due.compareTo(threshold) > 0) {
                    throw new ClaimStatusRefusedException("A claim whose due is over the note threshold needs a note");
                }
            }
        }
        if (change.action() == ClaimAction.OPEN
                && closedPeriods.atCap(claim.getSheetId(), claim.getPersonId(), claim.getDueBy())) {
            throw new ClaimStatusRefusedException("The person already has as many open claims in the period as its cap allows");
        }
        if (change.action() == ClaimAction.VOID && claim.getStatus() == ClaimStatus.SETTLED) {
            LocalDate settled = log.settledOn(claim);
            if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD || settled == null
                    || sheets.today(claim.getSheetId()).isAfter(settled.plusDays(SETTLED_VOID_DAYS))) {
                throw new ClaimStatusRefusedException("Only a steward can void a settled claim, and only within "
                        + SETTLED_VOID_DAYS + " days of its settlement");
            }
        }
        if (change.action() == ClaimAction.RESTORE) {
            LocalDate deleted = log.lastOn(claim, ClaimStatus.DELETED);
            if (deleted == null || !deleted.equals(sheets.today(claim.getSheetId()))) {
                throw new ClaimStatusRefusedException("A deleted claim can be restored only on the day it was deleted");
            }
        }
        String voidReason = null;
        if (change.action() == ClaimAction.VOID && !voidReasons.onSheet(claim.getSheetId()).isEmpty()) {
            if (change.reason() == null || !voidReasons.has(claim.getSheetId(), change.reason())) {
                throw new ClaimStatusRefusedException("A void needs a void reason from the sheet's list");
            }
            voidReason = change.reason();
        }
        if (change.action() == ClaimAction.SETTLE && claim.isDeferred()) {
            throw new ClaimStatusRefusedException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
        }
        if (change.action() == ClaimAction.SETTLE && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(change.user())) {
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        if (change.action() == ClaimAction.SETTLE && change.user().equals(claim.getEditedBy())) {
            throw new ClaimStatusRefusedException("The user who last edited a claim cannot settle it");
        }
        if (change.action() == ClaimAction.APPROVE) {
            if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(change.user())) {
                throw new ClaimStatusRefusedException("The user who opened a claim cannot approve it");
            }
            if (change.user().equals(claim.getEditedBy())) {
                throw new ClaimStatusRefusedException("The user who last edited a claim cannot approve it");
            }
            if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
                throw new ClaimStatusRefusedException("Only a steward can approve a pending claim");
            }
        }
        if (change.action() == ClaimAction.REVIEW && settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
            throw new ClaimStatusRefusedException("Only a steward can review a claim");
        }
        boolean review = false;
        if (change.action() == ClaimAction.OPEN) {
            BigDecimal threshold = settings.reviewThreshold(claim.getSheetId(), claim.getDate());
            if (threshold != null) {
                String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
                BigDecimal due = figures.due(claim, grade);
                review = due != null && due.compareTo(threshold) > 0;
            }
        }
        BigDecimal due = null;
        boolean pending = false;
        if (change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            // Figures.due: from the bound figure when a statement binds the claim (OPEN or PENDING), else live.
            due = figures.due(claim, grade);
            // Before the clerk limit: a claim refused for want of a proof never goes PENDING.
            BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), grade, claim.getDate());
            if (threshold != null && due != null && due.compareTo(threshold) > 0 && claim.getProofs() == 0) {
                throw new ClaimStatusRefusedException("A claim whose due is over the proof threshold needs a proof");
            }
            if (threshold != null && due != null && due.compareTo(threshold) > 0) {
                for (String kind : settings.proofKinds(claim.getSheetId())) {
                    if (claim.proofsOf(kind) == 0) {
                        throw new ClaimStatusRefusedException("A claim whose due is over the proof threshold needs a proof of each kind: " + kind);
                    }
                }
            }
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            pending = change.action() == ClaimAction.SETTLE && limit != null && due != null && due.compareTo(limit) > 0
                    && settings.role(claim.getSheetId(), change.user()) == Role.CLERK;
        }
        claim.setStatus(pending ? ClaimStatus.PENDING : review ? ClaimStatus.REVIEW : change.action().to());
        // Settled one at a time: in no batch (a bulk settle sets its batch after this).
        if (change.action() == ClaimAction.SETTLE) claim.setBatch(null);
        if (change.action() == ClaimAction.VOID) claim.setVoidReason(voidReason);
        if (change.action() == ClaimAction.OPEN) {
            // A claim gone to REVIEW gets its number only once a steward reviews it.
            if (!review) claim.setNumber(numbers.next(claim));
            claim.setOpenedBy(change.user());
        }
        if (change.action() == ClaimAction.REVIEW) claim.setNumber(numbers.next(claim));
        if ((change.action() == ClaimAction.SETTLE && !pending) || change.action() == ClaimAction.APPROVE) {
            claim.setDue(due);
        }
    }
}
