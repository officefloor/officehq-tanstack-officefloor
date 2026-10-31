package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimSplit {
    public void save(@Val ClaimSplit split, @Val Claim claim, Claims claims) {
        claims.save(claim);
        claims.save(split.into());
    }
}
