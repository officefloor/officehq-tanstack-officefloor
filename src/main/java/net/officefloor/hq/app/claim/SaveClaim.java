package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class SaveClaim {
    public void save(@Val Claim claim, ClaimRepository claims) throws InvalidClaimException {
        if (!claims.update(claim)) {
            throw new InvalidClaimException("Only a DRAFT claim can be edited");
        }
    }
}
