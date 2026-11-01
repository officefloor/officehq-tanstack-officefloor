package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimShare {
    public void save(@Val ClaimShareChange change, @Val Claim claim, Claims claims) {
        claims.share(claim, change.share());
    }
}
