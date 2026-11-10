package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Closes the user's editor on the claim (the cancel button): its lock, if theirs, is released. */
public class UnlockClaim {
    public void unlock(@Val Claim claim, ClaimRepository repository, HttpServletRequest request) {
        repository.unlock(claim, Audit.user(request));
    }
}
