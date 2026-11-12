package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Stores the OPEN claim deferred until the date; refused when another user moved it on first. */
public class ApplyDefer {
    public void apply(@Val DeferInput input, @Val Claim claim, ClaimStore store) throws InvalidSheetException {
        if (!store.defer(claim, input.until())) {
            throw new InvalidSheetException("Only an OPEN claim can be deferred");
        }
    }
}
