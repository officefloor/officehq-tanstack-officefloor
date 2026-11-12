package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim edited to an external reference another claim on the sheet carries, compared without regard to case. */
public class CheckClaimRef {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimStore claims) throws InvalidSheetException {
        CheckRef.refuseTaken(claims, claim.getSheetId(), input, claim.getId());
    }
}
