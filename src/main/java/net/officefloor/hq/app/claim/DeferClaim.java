package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Defers an OPEN claim until a date after the sheet's today. */
public class DeferClaim {
    public void apply(@Val Sheet sheet, @Val Claim claim, @Val Deferral deferral)
            throws InvalidClaimStatusException, InvalidClaimException {
        claim.defer(deferral.until(), sheet.today());
    }
}
