package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) any change to a claim whose due-by falls inside a closed period (its period is the one containing its due-by). */
public class CheckClaimPeriodOpen {
    public void check(@Val Claim claim, PeriodRepository periods) throws InvalidClaimException {
        Period closed = periods.closedOn(claim.sheetId(), claim.dueBy());
        if (closed != null) {
            throw new InvalidClaimException("The claim is due by " + claim.dueBy() + ", in the closed period " + closed.name() + ".");
        }
    }
}
