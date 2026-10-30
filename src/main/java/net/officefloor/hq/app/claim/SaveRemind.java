package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveRemind {
    public void save(@Val Claim claim, Claims claims) {
        claims.remind(claim);
    }
}
