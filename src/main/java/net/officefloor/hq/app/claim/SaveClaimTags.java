package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the saved claim's tags as the form gave them (none: cleared), adding each to the sheet's tag list. */
public class SaveClaimTags {
    public void save(@Val Claim claim, @Val ClaimInput input, ClaimRepository repository) {
        repository.saveTags(claim, input.tags());
    }
}
