package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class RefuseInvalidCorrectionClaim {
    // A claim edited into a correction must name, by number, a SETTLED claim of its own person.
    public void refuse(@Val ClaimInput input, @Val Claim claim, Claims claims) throws InvalidClaimException {
        RefuseInvalidCorrection.refuse(input, claim.sheetId(), claim.personId(), claims);
    }
}
