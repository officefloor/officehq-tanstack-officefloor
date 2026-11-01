package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimWaived {
    public void save(@Val Claim claim, Claims claims) {
        claims.waive(claim);
    }
}
