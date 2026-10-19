package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses opening, settling or voiding a claim dated inside a closed period. */
public class CheckClaimActionOpen {
    public void check(@Val Claim claim, PeriodRepository periods) throws InvalidClaimStatusException {
        var closed = periods.closedOn(claim.getSheetId(), claim.getDate());
        if (closed.isPresent()) {
            throw new InvalidClaimStatusException(ClosedPeriods.message(closed.get().name()));
        }
    }
}
