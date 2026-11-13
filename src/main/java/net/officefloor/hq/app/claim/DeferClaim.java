package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Defers the claim until the deferral's date. */
public class DeferClaim {
    public void defer(@Val Claim claim, @Val Deferral deferral, ClaimRepository repository, Out<Claim> deferred) {
        Claim next = claim.deferTo(deferral.until());
        repository.defer(next);
        deferred.set(next);
    }
}
