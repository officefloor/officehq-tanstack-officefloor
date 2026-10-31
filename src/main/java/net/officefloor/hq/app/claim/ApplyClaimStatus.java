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
 *  limit) it goes PENDING instead, its due still live; a steward settles anything. Only a steward
 *  approves a PENDING claim (it settles, its due fixed at its live figure then, plus its late charge, at least the floor),
 *  and never the user who opened it or last edited it; a rejection sends it back to OPEN, bound again by the last
 *  statement if that bound it.
 *  A claim whose due is above the proof threshold in force on its date (its person's grade's then when
 *  one is set, else the sheet's) cannot be settled or
 *  approved while it has no proofs (refused before the clerk limit is considered).
 *  A claim whose due is above the sheet's note threshold in force on its date cannot be opened while
 *  its note is empty.
 *  A deferred claim cannot be settled until the sheet's today reaches its deferred-until date.
 *  Only a steward may void a SETTLED claim, and only while the sheet's today is within 7 days of its
 *  settlement (its status log's SETTLED date); its frozen due then leaves every total.
 *  A claim due by in a closed period cannot change status at all. Opening, settling and voiding are
 *  only for the roles the sheet's permission matrix allows (see {@link Settings#allows}).
 *  While the sheet's list of void reasons is not empty, a void needs a reason from it, which the
 *  claim keeps. */
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
        if (change.action() == ClaimAction.VOID && claim.getStatus() == ClaimStatus.SETTLED) {
            LocalDate settled = log.settledOn(claim);
            if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD || settled == null
                    || sheets.today(claim.getSheetId()).isAfter(settled.plusDays(SETTLED_VOID_DAYS))) {
                throw new ClaimStatusRefusedException("Only a steward can void a settled claim, and only within "
                        + SETTLED_VOID_DAYS + " days of its settlement");
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
        BigDecimal due = null;
        boolean pending = false;
        if (change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            // Figures.due: from the bound figure for an OPEN claim, the live one for a PENDING claim.
            due = figures.due(claim, grade);
            // Before the clerk limit: a claim refused for want of a proof never goes PENDING.
            BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), grade, claim.getDate());
            if (threshold != null && due != null && due.compareTo(threshold) > 0 && claim.getProofs() == 0) {
                throw new ClaimStatusRefusedException("A claim whose due is over the proof threshold needs a proof");
            }
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            pending = change.action() == ClaimAction.SETTLE && limit != null && due != null && due.compareTo(limit) > 0
                    && settings.role(claim.getSheetId(), change.user()) == Role.CLERK;
        }
        claim.setStatus(pending ? ClaimStatus.PENDING : change.action().to());
        // Settled one at a time: in no batch (a bulk settle sets its batch after this).
        if (change.action() == ClaimAction.SETTLE) claim.setBatch(null);
        if (change.action() == ClaimAction.VOID) claim.setVoidReason(voidReason);
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(numbers.next(claim));
            claim.setOpenedBy(change.user());
        }
        if ((change.action() == ClaimAction.SETTLE && !pending) || change.action() == ClaimAction.APPROVE) {
            claim.setDue(due);
        }
    }
}
