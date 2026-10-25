package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Refuses an edited correction that does not name a SETTLED claim of its person, or a claim naming one that is not a correction. */
public class RefuseBadEditedCorrection {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimRepository repository) throws BadRequestException {
        RefuseBadCorrection.refuse(input, claim.getSheetId(), claim.getPersonId(), repository);
    }
}
