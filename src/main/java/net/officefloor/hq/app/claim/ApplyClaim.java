package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Claim claim) {
        claim.setAmount(input.amount());
        claim.setDate(input.date());
        claim.setNote(input.note());
        claim.setCorrects(input.corrects());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setEditedBy(input.user());
    }
}
