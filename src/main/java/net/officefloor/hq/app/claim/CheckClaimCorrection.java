package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a claim edited into a correction unless it names, by number, a SETTLED claim of its person, and no larger than what remains of it. */
public class CheckClaimCorrection {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimStore claims) throws InvalidSheetException {
        CheckCorrection.refuseUnsettled(claims, claim.getSheetId(), claim.getPersonId(), input, claim.getId());
    }
}
