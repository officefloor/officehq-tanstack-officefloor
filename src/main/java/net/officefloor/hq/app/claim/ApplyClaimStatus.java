package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyClaimStatus {
    // Stored only if the claim still has the status it was loaded with, so two users cannot both move it.
    public void apply(@PathVariable("status") String status, @Val Claim claim, Claims claims,
            Out<ClaimResponse> responseOut) throws ClaimStatusRefusedException {
        ClaimStatus next;
        try {
            next = ClaimStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ClaimStatusRefusedException("Not a claim status: " + status);
        }
        if (!claim.status().allows(next) || !claims.changeStatus(claim, next)) {
            throw new ClaimStatusRefusedException("A " + claim.status() + " claim cannot become " + next + ".");
        }
        responseOut.set(ClaimResponse.from(claim.withStatus(next)));
    }
}
