package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse moving a claim whose due-by falls inside a closed period (as a bulk move refuses it). */
public class RefuseClosedClaimMove {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.dueBy());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
