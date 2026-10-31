package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; voiding clears it (the number is not reused).
 *  Settling fixes the claim's due at the figure it has then: the one its person's last statement
 *  bound it to, else its person's grade's rate in force on the claim's date; with no rate it stays live. The user who opened a claim may not settle it.
 *  A claim dated in a closed period cannot change status at all. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim, Sheets sheets, People people,
            Figures figures, ClosedPeriods closedPeriods, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDate());
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        if (change.action() == ClaimAction.SETTLE && claim.getOpenedBy() != null
                && claim.getOpenedBy().equals(change.user())) {
            throw new ClaimStatusRefusedException("The user who opened a claim cannot settle it");
        }
        claim.setStatus(change.action().to());
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
            claim.setOpenedBy(change.user());
        }
        if (change.action() == ClaimAction.SETTLE) {
            String grade = people.find(claim.getSheetId(), claim.getPersonId()).getGrade();
            claim.setDue(claim.isStated() ? claim.getStatedDue() : figures.live(claim, grade));
        }
        number.set(new ClaimNumber(claim.getNumber()));
        if (change.action() == ClaimAction.VOID) {
            claim.setNumber(null);
        }
    }
}
