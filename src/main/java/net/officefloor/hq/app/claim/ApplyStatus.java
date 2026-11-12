package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the new status, refusing a change its current status does not allow. Opening
 *  gives the claim the sheet's next number; voiding clears it. */
public class ApplyStatus {
    public void apply(@Val StatusInput input, @Val Claim claim, ClaimStore store, Out<StatusChange> change)
            throws InvalidSheetException {
        ClaimStatus from = claim.getStatus();
        if (!from.canBecome(input.status()) || !store.move(claim, from, input.status())) {
            throw new InvalidSheetException("A " + from + " claim cannot become " + input.status());
        }
        claim.setStatus(input.status());
        if (input.status() == ClaimStatus.OPEN) {
            claim.setNumber(store.nextNumber(claim.getSheetId()));
        }
        change.set(new StatusChange(claim.getNumber()));
        if (input.status() == ClaimStatus.VOID) {
            claim.setNumber(null);
        }
    }
}
