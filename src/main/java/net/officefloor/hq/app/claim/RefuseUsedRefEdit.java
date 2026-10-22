package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Refuse an edit giving the claim a reference another claim on the sheet already has, compared without regard to case. */
public class RefuseUsedRefEdit {
    public void refuse(@Val ClaimInput input, @Val Claim loaded, ClaimRepository repository)
            throws InvalidClaimException {
        if (input.ref() != null && repository.refUsed(loaded.sheetId(), input.ref(), loaded.id())) {
            throw new InvalidClaimException(RefuseUsedNewRef.message(input.ref()));
        }
    }
}
