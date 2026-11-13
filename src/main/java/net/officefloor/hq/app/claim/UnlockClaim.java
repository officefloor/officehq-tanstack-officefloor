package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Releases the user's lock on the claim (its editor was saved or cancelled); nothing when they hold none. */
public class UnlockClaim {
    public void unlock(@Val Claim claim, @RequestParam("user") String user, ClaimLockRepository locks) {
        locks.unlock(claim.sheetId(), claim.id(), user);
    }
}
