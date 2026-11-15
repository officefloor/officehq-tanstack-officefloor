package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Closes a claim's editor (cancelled or saved): releases its lock when the user holds it. */
public class UnlockClaim {
    public void unlock(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            ClaimRepository claims) {
        claims.unlock(claim, by == null ? "" : by);
    }
}
