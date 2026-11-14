package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveReassignment {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.reassign(claim);
    }
}
