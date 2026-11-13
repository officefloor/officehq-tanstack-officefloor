package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Moves the claim to the action's status; refused (400) when the claim's status does not allow it. */
public class ApplyClaimAction {
    public void apply(@PathVariable("action") String action, @Val Claim claim, Out<ClaimAction> applied,
            Out<Claim> updated) throws InvalidClaimException {
        ClaimAction found = ClaimAction.of(action);
        if (found == null) {
            throw new InvalidClaimException("No such claim action: " + action);
        }
        if (!found.allowedFrom(claim.status())) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be " + found.to() + ".");
        }
        applied.set(found);
        updated.set(claim.withStatus(found.to()));
    }
}
