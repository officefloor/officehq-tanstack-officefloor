package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class RefuseUsedRefClaim {
    // A claim being edited may keep its own reference, but not take one another claim of the sheet carries (ignoring case).
    public void refuse(@Val ClaimInput input, @Val Claim claim, Claims claims) throws InvalidClaimException {
        RefuseUsedRef.refuse(input.ref(), claim.sheetId(), claim.id(), claims);
    }
}
