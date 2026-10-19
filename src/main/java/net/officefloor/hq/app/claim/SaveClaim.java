package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaim {
    public void save(@Val Claim claim, ClaimRepository repository) {
        repository.save(claim);
    }
}
