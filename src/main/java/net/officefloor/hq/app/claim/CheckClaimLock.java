package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses an edit of a claim whose editor another user has open, before anything is written. */
public class CheckClaimLock {
    public void check(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository,
            HttpServletRequest request) throws ClaimLockedException {
        if (repository.lockedByOther(claim, Audit.user(request), sheet.today())) {
            throw new ClaimLockedException(String.valueOf(claim.getId()));
        }
    }
}
