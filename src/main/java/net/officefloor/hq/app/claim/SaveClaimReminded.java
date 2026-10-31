package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimReminded {
    public void save(@Val Claim claim, Claims claims) {
        claims.remind(claim);
    }
}
