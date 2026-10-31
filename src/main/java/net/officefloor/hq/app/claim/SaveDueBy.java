package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveDueBy {
    public void save(@Val Claim claim, Claims claims) {
        claims.setDueBy(claim);
    }
}
