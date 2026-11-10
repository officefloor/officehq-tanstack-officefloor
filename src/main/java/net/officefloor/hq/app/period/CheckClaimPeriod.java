package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a change to a claim whose due-by (its period) is inside a closed period, before anything is written. */
public class CheckClaimPeriod {
    public void check(@Val Sheet sheet, @Val Claim claim, PeriodRepository periods) throws ClosedPeriodException {
        Period closed = periods.closedOn(sheet.id(), claim.getDueBy());
        if (closed != null) throw new ClosedPeriodException(closed.name());
    }
}
