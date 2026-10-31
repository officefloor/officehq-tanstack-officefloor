package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import java.math.BigDecimal;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; a voided claim keeps it (and it is never reused).
 *  Settling fixes the claim's due at the figure it has then: the one its person's last statement
 *  bound it to, else its person's grade's rate in force on the claim's date, at least the sheet's floor; with no rate it stays live. The user who opened a claim may not settle it.
 *  A clerk may settle a claim only when that due is at or below the sheet's clerk limit (a due of n/a
 *  is within any limit); a steward settles anything.
 *  A claim dated in a closed period cannot change status at all. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim, Sheets sheets, People people,
            Figures figures, ClosedPeriods closedPeriods, Settings settings)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        if (change.action() == ClaimAction.SETTLE && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(change.user())) {
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        BigDecimal due = null;
        if (change.action() == ClaimAction.SETTLE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            due = claim.isStated() ? claim.getStatedDue() : figures.live(claim, grade);
            BigDecimal limit = settings.clerkLimit(claim.getSheetId());
            if (limit != null && due != null && due.compareTo(limit) > 0
                    && settings.role(claim.getSheetId(), change.user()) == Role.CLERK) {
                throw new ClaimStatusRefusedException("A clerk cannot settle a claim whose due is over the clerk limit");
            }
        }
        claim.setStatus(change.action().to());
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
            claim.setOpenedBy(change.user());
        }
        if (change.action() == ClaimAction.SETTLE) {
            claim.setDue(due);
        }
    }
}
