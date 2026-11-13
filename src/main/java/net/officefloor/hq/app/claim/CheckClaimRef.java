package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses (400) an edit giving the claim an external reference another claim on the sheet already has, ignoring case. */
public class CheckClaimRef {
    public void check(@Val ClaimInput input, @Val Claim claim, ClaimRepository claims) throws InvalidClaimException {
        if (input.ref() != null && claims.refUsed(claim.sheetId(), input.ref(), claim.id())) {
            throw new InvalidClaimException("The reference " + input.ref() + " is already used on the sheet.");
        }
    }
}
