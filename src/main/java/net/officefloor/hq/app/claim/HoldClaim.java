package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Toggles the claim's hold: holds it when it is not held, else releases it. */
public class HoldClaim {
    public void hold(@Val Claim claim, ClaimRepository repository, Out<Claim> updated) {
        repository.hold(claim, !claim.held());
        updated.set(repository.find(claim.sheetId(), claim.id()));
    }
}
