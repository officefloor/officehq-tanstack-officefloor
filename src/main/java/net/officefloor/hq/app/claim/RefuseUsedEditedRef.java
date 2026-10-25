package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Val;

/** Refuses an edited claim whose reference another claim on the sheet already carries. */
public class RefuseUsedEditedRef {
    public void check(@Val Claim claim, @Val ClaimInput input, ClaimRepository repository) throws BadRequestException {
        RefuseUsedRef.refuse(claim.getSheetId(), input.ref(), claim.getId(), repository);
    }
}
