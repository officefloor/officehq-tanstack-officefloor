package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Defers an OPEN claim to a date after the sheet's today; not a claim dated inside a closed period. */
public class ApplyDefer {
    public void apply(@Val ValidDefer request, @Val Claim claim, Sheets sheets, Periods periods)
            throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.OPEN) {
            throw new ClaimStatusRefusedException("Only an OPEN claim can be deferred");
        }
        if (!request.until().isAfter(sheets.today(claim.getSheetId()))) {
            throw new ClaimStatusRefusedException("A claim can only be deferred to a date after the sheet's today");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can be deferred");
        }
        claim.setDeferredUntil(request.until());
    }
}
