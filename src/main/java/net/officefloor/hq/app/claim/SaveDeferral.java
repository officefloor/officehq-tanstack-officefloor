package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveDeferral {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.updateDeferral(claim);
    }
}
