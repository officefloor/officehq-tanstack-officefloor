package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import java.math.BigDecimal;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim its number (see {@link ClaimNumbers}); a voided claim keeps it (and it is never reused).
 *  Settling fixes the claim's due at the figure it has then: the one its person's last statement
 *  bound it to, else its person's grade's rate in force on the claim's date, at least the sheet's floor, plus its late charge then; with no rate it stays live. The user who opened a claim may not settle it.
 *  When a clerk settles a claim whose due is over the sheet's clerk limit (a due of n/a is within any
 *  limit) it goes PENDING instead, its due still live; a steward settles anything. Only a steward
 *  approves a PENDING claim (it settles, its due fixed at its live figure then, plus its late charge),
 *  and never the user who opened it; a rejection sends it back to OPEN, bound again by the last
 *  statement if that bound it.
 *  A claim whose due is above the sheet's proof threshold in force on its date cannot be settled or
 *  approved while it has no proofs (refused before the clerk limit is considered).
 *  A deferred claim cannot be settled until the sheet's today reaches its deferred-until date.
 *  A claim dated in a closed period cannot change status at all. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim, ClaimNumbers numbers, People people,
            Figures figures, ClosedPeriods closedPeriods, Settings settings)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        if (change.action() == ClaimAction.SETTLE && claim.isDeferred()) {
            throw new ClaimStatusRefusedException("A deferred claim cannot be settled until " + claim.getDeferredUntil());
        }
        if (change.action() == ClaimAction.SETTLE && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(change.user())) {
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        if (change.action() == ClaimAction.APPROVE) {
            if (claim.getOpenedBy() != null && claim.getOpenedBy().equals(change.user())) {
                throw new ClaimStatusRefusedException("The user who opened a claim cannot approve it");
            }
            if (settings.role(claim.getSheetId(), change.user()) != Role.STEWARD) {
                throw new ClaimStatusRefusedException("Only a steward can approve a pending claim");
            }
        }
        BigDecimal due = null;
        boolean pending = false;
        if (change.action() == ClaimAction.SETTLE || change.action() == ClaimAction.APPROVE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).gradeOn(claim.getDate());
            // Figures.figure: the bound figure for an OPEN claim, the live one for a PENDING claim.
            due = figures.figure(claim, grade);
            if (due != null) due = due.add(figures.charge(claim, due));
            // Before the clerk limit: a claim refused for want of a proof never goes PENDING.
            BigDecimal threshold = settings.proofThreshold(claim.getSheetId(), claim.getDate());
            if (threshold != null && due != null && due.compareTo(threshold) > 0 && claim.getProofs() == 0) {
                throw new ClaimStatusRefusedException("A claim whose due is over the proof threshold needs a proof");
            }
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            pending = change.action() == ClaimAction.SETTLE && limit != null && due != null && due.compareTo(limit) > 0
                    && settings.role(claim.getSheetId(), change.user()) == Role.CLERK;
        }
        claim.setStatus(pending ? ClaimStatus.PENDING : change.action().to());
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(numbers.next(claim));
            claim.setOpenedBy(change.user());
        }
        if ((change.action() == ClaimAction.SETTLE && !pending) || change.action() == ClaimAction.APPROVE) {
            claim.setDue(due);
        }
    }
}
