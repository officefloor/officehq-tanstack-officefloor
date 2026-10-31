package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Moves the claim to the action's status, refusing a change its current status does not allow. */
public class ApplyClaimStatus {
    public void apply(@Val ClaimStatusChange change, @Val Claim claim) throws ClaimStatusRefusedException {
        if (!change.action().allowedFrom(claim.getStatus())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), change.action());
        }
        claim.setStatus(change.action().to());
    }
}
