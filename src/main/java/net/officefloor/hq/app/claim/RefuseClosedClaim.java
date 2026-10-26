package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;

public class RefuseClosedClaim {
    // A claim dated inside a closed period cannot be edited, opened, settled or voided.
    public void refuse(@Val Claim claim, Periods periods) throws ClaimInClosedPeriodException {
        Period closed = periods.closedOn(claim.sheetId(), claim.date());
        if (closed != null) {
            throw new ClaimInClosedPeriodException("The period " + closed.name() + " is closed: its claims cannot change.");
        }
    }
}
