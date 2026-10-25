package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim, ClaimRepository repository, SheetRepository sheets) {
        claim.setAmount(input.amount());
        claim.setDate(input.date() != null ? input.date() : sheets.today(claim.getSheetId()));
        claim.setNote(input.note());
        repository.update(claim);
    }
}
