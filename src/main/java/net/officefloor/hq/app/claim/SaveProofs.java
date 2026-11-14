package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveProofs {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.updateProofs(claim);
    }
}
