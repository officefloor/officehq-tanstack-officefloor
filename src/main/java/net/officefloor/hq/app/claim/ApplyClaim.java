package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, SheetClock clock) {
        claim.setAmount(request.amount());
        claim.setDate(request.date() != null ? request.date() : clock.today(claim.getSheetId()));
        claim.setNote(request.note());
    }
}
