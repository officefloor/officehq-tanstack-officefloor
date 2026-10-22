package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse moving a claim dated inside a closed period, or whose due-by falls inside one. */
public class RefuseClosedClaimMove {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.date());
        if (closed == null) {
            closed = periods.closedOn(loaded.sheetId(), loaded.dueBy());
        }
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
