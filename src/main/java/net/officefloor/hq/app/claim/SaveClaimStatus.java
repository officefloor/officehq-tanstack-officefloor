package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaimStatus {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.updateStatus(claim);
    }
}
