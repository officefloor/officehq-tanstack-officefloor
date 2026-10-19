package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow.
 *  Opening gives the claim the sheet's next number; voiding clears it. Publishes the number the
 *  audit line carries (for a void, the number the claim had). */
public class ApplyClaimAction {
    public void apply(@Val ClaimAction action, @Val Claim claim, ClaimRepository repository,
            Out<ClaimNumber> numberOut) throws InvalidClaimStatusException {
        if (!action.allowedFrom(claim.getStatus())) {
            throw new InvalidClaimStatusException(
                    "A " + claim.getStatus() + " claim cannot be moved to " + action.to() + ".");
        }
        Integer had = claim.getNumber();
        claim.setStatus(action.to());
        switch (action) {
            case OPEN -> claim.setNumber(repository.nextNumber(claim.getSheetId()));
            case VOID -> claim.setNumber(null);
            default -> { }
        }
        numberOut.set(new ClaimNumber(action == ClaimAction.VOID ? had : claim.getNumber()));
    }
}
