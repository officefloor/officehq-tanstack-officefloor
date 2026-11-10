package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Opens the claim, recording the signed-in user who opened it. */
public class OpenClaim {
    public void apply(@Val Claim claim, HttpServletRequest request) throws InvalidClaimStatusException {
        claim.moveTo(ClaimStatus.OPEN);
        claim.setOpenedBy(Audit.user(request));
    }
}
