package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the claim's status, as it is once saved, to its status log. */
public class LogClaimStatus {
    public void log(@Val Claim claim, @RequestParam(value = "user", required = false) String user, ClaimLog log) {
        log.record(claim, user);
    }
}
