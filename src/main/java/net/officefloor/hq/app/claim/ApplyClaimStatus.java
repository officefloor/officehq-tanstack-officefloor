package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class ApplyClaimStatus {
    public void apply(@Val ValidClaimStatus request, @Val Claim claim) throws ClaimStatusRefusedException {
        if (!claim.getStatus().canBecome(request.target())) {
            throw new ClaimStatusRefusedException(claim.getStatus(), request.target());
        }
        claim.setStatus(request.target());
    }
}
