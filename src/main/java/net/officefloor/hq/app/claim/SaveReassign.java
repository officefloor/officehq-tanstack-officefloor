package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveReassign {
    public void save(@Val Claim claim, Claims claims) {
        claims.reassign(claim);
    }
}
