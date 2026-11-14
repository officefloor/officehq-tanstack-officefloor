package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, SheetRepository sheets) {
        claim.setAmount(request.amount().toPlainString());
        claim.setDate((request.date() != null ? request.date() : sheets.today(claim.getSheetId())).toString());
        claim.setNote(request.note());
    }
}
