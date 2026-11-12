package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores the claim held, or released when it was held; refused when another user moved it on or toggled it first. */
public class ApplyHoldClaim {
    public void apply(@Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (!store.toggleHeld(claim)) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim can be held or released");
        }
    }
}
