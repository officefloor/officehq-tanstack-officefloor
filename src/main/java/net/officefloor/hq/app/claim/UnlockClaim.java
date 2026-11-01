package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Cancels the user's editor on the claim: releases its lock when the user's editor holds it. */
public class UnlockClaim {
    public void unlock(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims) {
        claims.unlock(sheet, claim.id(), user);
    }
}
