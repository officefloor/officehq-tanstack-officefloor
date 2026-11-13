package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses (400) any change to a claim dated inside a closed period. */
public class CheckClaimPeriodOpen {
    public void check(@Val Claim claim, PeriodRepository periods) throws InvalidClaimException {
        Period closed = periods.closedOn(claim.sheetId(), claim.date());
        if (closed != null) {
            throw new InvalidClaimException("The claim is dated in the closed period " + closed.name() + ".");
        }
    }
}
