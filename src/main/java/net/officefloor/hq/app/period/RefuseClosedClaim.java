package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse opening, settling, voiding, moving or deferring a claim dated inside a closed period. */
public class RefuseClosedClaim {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.date());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
