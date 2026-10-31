package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Defers an OPEN claim until a date after the sheet's today (a deferred one may be deferred again).
 *  A claim not OPEN, a date not after today, or a claim due by in a closed period is refused. */
public class ApplyClaimDefer {
    public void apply(@Val ClaimDefer change, @Val Claim claim, Sheets sheets, ClosedPeriods closedPeriods)
            throws ClaimStatusRefusedException, PeriodClosedException {
        closedPeriods.refuse(claim.getSheetId(), claim.getDueBy());
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new ClaimStatusRefusedException("Only an OPEN claim can be deferred");
        }
        if (!change.until().isAfter(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusRefusedException("A claim can only be deferred to a date after the sheet's today");
        }
        claim.setDeferredUntil(change.until());
        claim.setDeferred(true);
    }
}
