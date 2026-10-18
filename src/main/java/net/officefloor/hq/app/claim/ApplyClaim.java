package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Sets the claim's amount, date, note and the number it corrects; a claim given no date takes the sheet's today. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim, SheetRepository sheets) {
        claim.setAmount(input.amount());
        claim.setDate(input.date() != null ? input.date()
                : sheets.findById(claim.getSheetId()).orElseThrow().getToday());
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
    }
}
