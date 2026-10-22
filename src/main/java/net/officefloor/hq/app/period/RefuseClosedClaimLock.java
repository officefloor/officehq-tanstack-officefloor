package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.plugin.variable.Val;

/** Refuse opening the editor of (locking) a claim dated inside a closed period. Saving the edit is then refused
 *  when its due-by falls inside one (see RefuseClosedClaimEdit). */
public class RefuseClosedClaimLock {
    public void refuse(@Val Claim loaded, PeriodRepository periods) throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.date());
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
