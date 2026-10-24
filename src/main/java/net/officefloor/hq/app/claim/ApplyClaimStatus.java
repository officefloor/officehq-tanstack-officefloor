package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Moves the claim to the status the action leads to; refuses a change its status does not allow. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("action") String action, @Val Claim claim, ClaimRepository repository)
            throws ClaimStatusException {
        ClaimStatus next = claim.getStatus().after(action);
        if (next == null) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot " + action + ".");
        }
        claim.setStatus(next);
        repository.saveAndFlush(claim);
    }
}
