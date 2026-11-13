package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the saved claim's external reference as the form gave it (blank: none). */
public class SaveClaimRef {
    public void save(@Val Claim claim, @Val ClaimInput input, ClaimRepository repository) {
        repository.saveRef(claim, input.ref());
    }
}
