package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, Sheets sheets) {
        claim.setAmount(request.amount());
        claim.setDate(request.date() != null ? request.date() : sheets.today(claim.getSheetId()));
        claim.setNote(request.note());
    }
}
