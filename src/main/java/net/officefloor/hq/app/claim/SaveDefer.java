package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveDefer {
    public void save(@Val Claim claim, Claims claims) {
        claims.defer(claim);
    }
}
