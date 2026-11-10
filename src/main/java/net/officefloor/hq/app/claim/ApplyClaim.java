package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim)
            throws InvalidClaimStatusException {
        claim.checkEditable();
        claim.setAmount(input.amount());
        claim.setDate(input.date() == null ? sheet.today() : input.date());
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
        claim.setRef(input.ref());
    }
}
