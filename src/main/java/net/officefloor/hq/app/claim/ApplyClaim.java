package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimValues values, @Val Claim claim, Sheets sheets) {
        claim.setAmount(values.amount());
        claim.setDate(values.date() != null ? values.date() : sheets.today(claim.getSheetId()));
        claim.setNote(values.note());
    }
}
