package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Refuses the change when the claim, as it stands, is dated inside a closed period of its sheet. */
public class CheckClaimPeriod {
    public void check(@Val Claim claim, PeriodRepository periods) throws ClosedPeriodException {
        var closed = periods.findBySheetId(claim.getSheetId()).stream()
                .filter((p) -> p.isClosed() && p.contains(claim.getDate())).findFirst();
        if (closed.isPresent()) {
            throw new ClosedPeriodException("The claim is dated inside the closed period " + closed.get().getName() + ".");
        }
    }
}
