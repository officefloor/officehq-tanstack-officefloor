package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse re-dating a claim whose due-by falls inside a closed period. Its due-by is fixed at creation, so the new
 *  date does not move it into (or out of) a period. */
public class RefuseClosedClaimRedate {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.dueBy());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
