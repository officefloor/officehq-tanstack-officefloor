package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse an edit of a claim whose due-by falls inside a closed period. Its due-by is fixed at creation, so a new
 *  date does not move it into (or out of) a period. */
public class RefuseClosedClaimEdit {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.dueBy());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
