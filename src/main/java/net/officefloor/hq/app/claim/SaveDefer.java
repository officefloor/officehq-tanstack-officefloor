package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Stores the deferral, but only while the claim is still OPEN; a change another user made first is refused. */
public class SaveDefer {
    public void save(@Val Claim claim, ClaimRepository claims, Out<Claim> saved) throws InvalidClaimStatusException {
        if (!claims.defer(claim)) {
            throw new InvalidClaimStatusException("The claim was changed by someone else");
        }
        saved.set(claim);
    }
}
