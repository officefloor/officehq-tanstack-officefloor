package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuses editing a claim into a correction that does not name a SETTLED claim of its person. */
public class CheckClaimEditCorrection {
    public void check(@Val ValidClaim request, @Val Claim claim, ClaimRepository claims) throws InvalidClaimException {
        Correction.refuse(request, claim.getSheetId(), claim.getPersonId(), claims);
    }
}
