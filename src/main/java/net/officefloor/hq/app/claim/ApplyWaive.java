package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores the claim's late charge waived; refused when another user moved it on or waived it first. */
public class ApplyWaive {
    public void apply(@Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (!store.waive(claim)) {
            throw new InvalidSheetException("Only an OPEN or PENDING claim not yet waived can have its late charge waived");
        }
    }
}
