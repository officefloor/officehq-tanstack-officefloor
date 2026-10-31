package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; voiding clears it (the number is not reused). */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim, Sheets sheets, Out<ClaimNumber> number)
            throws ClaimStatusRefusedException {
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        claim.setStatus(change.action().to());
        if (change.action() == ClaimAction.OPEN) {
            claim.setNumber(sheets.nextClaimNumber(claim.getSheetId()));
        }
        number.set(new ClaimNumber(claim.getNumber()));
        if (change.action() == ClaimAction.VOID) {
            claim.setNumber(null);
        }
    }
}
