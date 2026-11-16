package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the edit and releases the claim's lock; refused when the claim is no longer a DRAFT or another user holds
 *  its lock (has its editor open). */
public class SaveClaim {
    public void save(@Val Claim claim, @Val ClaimValues values, ClaimRepository claims) throws InvalidClaimException {
        if (!claims.update(claim, values.by())) {
            throw new InvalidClaimException("Only a DRAFT claim no one else is editing can be edited");
        }
    }
}
