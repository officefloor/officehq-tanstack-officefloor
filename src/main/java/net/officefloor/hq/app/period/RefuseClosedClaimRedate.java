package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.RedateInput;
import net.officefloor.plugin.variable.Val;

/** Refuse re-dating a claim dated inside a closed period, or to a new date inside one. */
public class RefuseClosedClaimRedate {
    public void refuse(@Val RedateInput input, @Val Claim loaded, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(loaded.sheetId());
        Period closed = periods.closedOn(loaded.sheetId(), loaded.date());
        if (closed == null) {
            closed = periods.closedOn(loaded.sheetId(), input.date());
        }
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
