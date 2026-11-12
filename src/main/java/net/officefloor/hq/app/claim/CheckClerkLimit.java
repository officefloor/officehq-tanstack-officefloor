package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rate.RateStore;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Works out the move a status change makes. Opening a DRAFT whose due is above the review threshold in
 *  force on its date sends it to REVIEW instead; only a steward's review moves a REVIEW claim on, to OPEN,
 *  and otherwise it may only be voided. A clerk settling an OPEN claim whose due is above the
 *  sheet's clerk limit sends it to PENDING instead. A claim whose due is n/a is within any limit; a
 *  sheet with no limit, and a steward, settle anything. Only a steward reviews a PENDING claim, and
 *  only a review moves it to SETTLED or back to OPEN; a review of a claim that is not PENDING is refused.
 *  Before the limit is considered, settling or approving a claim with no proofs whose due is above the
 *  proof threshold is refused (see {@link CheckProofs}), so it never goes to PENDING. Opening, settling
 *  and voiding are refused to a role the sheet's permission matrix does not allow it, and opening
 *  a claim of a person whose flag is in force on the sheet's today to anyone but a steward. A SETTLED claim
 *  may be voided only by a steward, and only within {@link #VOID_SETTLED_DAYS} days of the sheet's today
 *  it was settled on (its status log's SETTLED date). A DELETED claim may be restored only on the
 *  sheet's today it was deleted on. */
public class CheckClerkLimit {
    /** How many days after settling a claim a steward may still void it. */
    static final int VOID_SETTLED_DAYS = 7;

    public void check(@Val StatusInput input, @Val Claim claim, RoleStore roles, SettingStore settings,
            PersonStore people, RateStore rates, SheetStore sheets, ClaimStore claims, ClaimLogStore log,
            Out<StatusMove> move)
            throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        Role role = roles.find(claim.getSheetId(), input.user());
        boolean steward = role == Role.STEWARD;
        if (input.review() && from == ClaimStatus.REVIEW) {
            if (!steward) {
                throw new InvalidSheetException("Only a steward can review a claim");
            }
            if (input.status() != ClaimStatus.OPEN) {
                throw new InvalidSheetException("A review makes a REVIEW claim OPEN");
            }
            move.set(new StatusMove(from, ClaimStatus.OPEN));
            return;
        }
        if (input.review()) {
            if (from != ClaimStatus.PENDING) {
                throw new InvalidSheetException("Only a PENDING claim can be approved or rejected");
            }
            if (!steward) {
                throw new InvalidSheetException("Only a steward can approve or reject a claim");
            }
            if (input.status() == ClaimStatus.OPEN && input.reason().isEmpty()) {
                throw new InvalidSheetException("A rejection needs a reason");
            }
            CheckProofs.refuseUnproved(input, claim, settings, people, rates, sheets, claims);
            move.set(new StatusMove(from, input.status()));
            return;
        }
        if (from == ClaimStatus.PENDING && input.status() != ClaimStatus.VOID) {
            throw new InvalidSheetException("A PENDING claim waits for a steward to approve or reject it");
        }
        if (from == ClaimStatus.REVIEW && input.status() != ClaimStatus.VOID) {
            throw new InvalidSheetException("A REVIEW claim waits for a steward to review it");
        }
        refuseUnpermitted(input.status(), claim.getSheetId(), role, settings);
        if (from == ClaimStatus.SETTLED && input.status() == ClaimStatus.VOID) {
            refuseLateVoid(claim, steward, log, sheets);
        }
        if (from == ClaimStatus.DELETED && input.status() == ClaimStatus.DRAFT) {
            refuseLateRestore(claim, log, sheets);
        }
        if (from == ClaimStatus.DRAFT && input.status() == ClaimStatus.OPEN) {
            refuseFlagged(claim, steward, people, sheets);
            CheckProofs.refuseUnnoted(claim, settings, people, rates, sheets, claims);
        }
        CheckProofs.refuseUnproved(input, claim, settings, people, rates, sheets, claims);
        ClaimStatus to = overLimit(input, claim, steward, settings, people, rates, sheets, claims)
                ? ClaimStatus.PENDING : input.status();
        if (from == ClaimStatus.DRAFT && to == ClaimStatus.OPEN
                && overReview(claim, settings, people, rates, sheets, claims)) {
            to = ClaimStatus.REVIEW;
        }
        move.set(new StatusMove(from, to));
    }

    /** Whether opening the claim sends it to REVIEW: its due is above the review threshold in force on its
     *  date. A sheet with no threshold then, and a claim whose due is n/a, open as OPEN. */
    static boolean overReview(Claim claim, SettingStore settings, PersonStore people, RateStore rates,
            SheetStore sheets, ClaimStore claims) {
        BigDecimal threshold = settings.reviewThreshold(claim.getSheetId(), claim.getDate());
        if (threshold == null) {
            return false;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.forClaim(claim).find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal due = Lateness.of(claim.getSheetId(), settings, sheets, claims, people).due(claim, pct);
        return due != null && due.compareTo(threshold) > 0;
    }

    /** Refuses voiding a SETTLED claim by anyone but a steward, or later than {@link #VOID_SETTLED_DAYS}
     *  days after it was settled. */
    static void refuseLateVoid(Claim claim, boolean steward, ClaimLogStore log, SheetStore sheets)
            throws InvalidSheetException {
        if (!steward) {
            throw new InvalidSheetException("Only a steward can void a SETTLED claim");
        }
        LocalDate settled = log.settledOn(claim);
        LocalDate today = sheets.today(claim.getSheetId());
        if (settled == null || today == null || today.isAfter(settled.plusDays(VOID_SETTLED_DAYS))) {
            throw new InvalidSheetException("A SETTLED claim can be voided only within " + VOID_SETTLED_DAYS
                    + " days of settling it");
        }
    }

    /** Refuses restoring a DELETED claim on any day but the sheet's today it was deleted on (its status
     *  log's last DELETED date). */
    static void refuseLateRestore(Claim claim, ClaimLogStore log, SheetStore sheets) throws InvalidSheetException {
        LocalDate deleted = log.lastOn(claim, ClaimStatus.DELETED);
        LocalDate today = sheets.today(claim.getSheetId());
        if (deleted == null || today == null || !today.equals(deleted)) {
            throw new InvalidSheetException("A DELETED claim can be restored only on the day it was deleted");
        }
    }

    /** Refuses opening a claim by anyone but a steward while its person's flag is in force on the sheet's today. */
    static void refuseFlagged(Claim claim, boolean steward, PersonStore people, SheetStore sheets)
            throws InvalidSheetException {
        if (!steward && flagged(claim, people, sheets)) {
            throw new InvalidSheetException("Only a steward can open a claim of a flagged person");
        }
    }

    /** Whether the claim's person has a flag in force on the sheet's today. */
    static boolean flagged(Claim claim, PersonStore people, SheetStore sheets) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        return person != null && person.flaggedOn(sheets.today(claim.getSheetId()));
    }

    /** Refuses opening, settling or voiding a claim by a role the sheet's permission matrix does not allow it. */
    static void refuseUnpermitted(ClaimStatus to, String sheetId, Role role, SettingStore settings)
            throws InvalidSheetException {
        String action = to == ClaimStatus.OPEN ? "open" : to == ClaimStatus.SETTLED ? "settle"
                : to == ClaimStatus.VOID ? "void" : null;
        if (action != null && !settings.allows(sheetId, action, role)) {
            throw new InvalidSheetException("A " + role.shown() + " may not " + action + " a claim");
        }
    }

    private static boolean overLimit(StatusInput input, Claim claim, boolean steward, SettingStore settings,
            PersonStore people, RateStore rates, SheetStore sheets, ClaimStore claims) {
        if (input.status() != ClaimStatus.SETTLED || claim.getStatus() != ClaimStatus.OPEN || steward) {
            return false;
        }
        BigDecimal limit = settings.clerkLimit(claim.getSheetId());
        if (limit == null) {
            return false;
        }
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        BigDecimal pct = person == null ? null : rates.forClaim(claim).find(claim.getSheetId(), person.gradeOn(claim.getDate()), claim.getDate());
        BigDecimal due = Lateness.of(claim.getSheetId(), settings, sheets, claims, people).due(claim, pct);
        return due != null && due.compareTo(limit) > 0;
    }
}
