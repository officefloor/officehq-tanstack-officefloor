package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Saves the claim and, when its status changed (or it is new), writes the line to its status log. */
public class SaveClaim {
    public void save(@Val Claim claim, @HttpHeaderParameter("X-User") String user, ClaimRepository repository,
            ClaimLog log) {
        repository.save(claim);
        log.record(claim, user);
    }
}
