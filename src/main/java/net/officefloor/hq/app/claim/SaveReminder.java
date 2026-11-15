package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveReminder {
    public void save(@Val Claim claim, ClaimRepository claims) {
        claims.updateReminded(claim);
        if (claim.isEscalating()) {
            claims.markEscalated(claim);
        }
    }
}
