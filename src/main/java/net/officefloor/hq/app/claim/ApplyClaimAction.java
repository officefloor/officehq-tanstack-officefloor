package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow. */
public class ApplyClaimAction {
    public void apply(@Val ClaimAction action, @Val Claim claim) throws InvalidClaimStatusException {
        if (!action.allowedFrom(claim.getStatus())) {
            throw new InvalidClaimStatusException(
                    "A " + claim.getStatus() + " claim cannot be moved to " + action.to() + ".");
        }
        claim.setStatus(action.to());
    }
}
