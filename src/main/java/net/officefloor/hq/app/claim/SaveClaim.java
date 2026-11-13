package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveClaim {
    public void save(@Val Claim claim, ClaimRepository repository, Out<Claim> saved) {
        saved.set(repository.save(claim));
    }
}
