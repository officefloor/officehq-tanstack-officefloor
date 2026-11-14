package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Waives the claim's late charge: it bears none from then on. */
public class WaiveClaim {
    public void waive(@Val Claim claim, ClaimRepository repository, Out<Claim> updated) {
        repository.waive(claim);
        updated.set(repository.find(claim.sheetId(), claim.id()));
    }
}
