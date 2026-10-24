package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, ClaimRepository repository) {
        claim.setAmount(input.amount());
        claim.setDate(input.date() != null ? input.date() : sheet.getToday());
        claim.setNote(input.note());
        repository.save(claim);
    }
}
