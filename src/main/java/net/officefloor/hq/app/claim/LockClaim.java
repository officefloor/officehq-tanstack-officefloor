package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Locks the claim for the user whose editor is now open on it. */
public class LockClaim {
    public void lock(@Val Claim claim, @RequestParam("user") String user, ClaimLockRepository locks) {
        locks.lock(claim.sheetId(), claim.id(), user);
    }
}
