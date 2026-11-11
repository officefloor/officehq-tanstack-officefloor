package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaim {
    public void save(@Val Claim claim, ClaimStore store) {
        store.save(claim);
    }
}
