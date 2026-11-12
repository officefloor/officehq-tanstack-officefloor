package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;

/** Stores the claim's new status and logs it, by the user, at its due then. */
public class SaveClaim {
    public void save(@Val StatusInput input, @Val Claim claim, ClaimStore store, ClaimLogStore log) {
        store.save(claim);
        log.record(claim, input.user());
    }
}
