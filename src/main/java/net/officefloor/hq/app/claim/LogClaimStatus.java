package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Write step after the claim is saved: logs the status it now has (see {@link ClaimLogRepository}). */
public class LogClaimStatus {
    public void log(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimLogRepository log) {
        log.record(claim, by);
    }
}
