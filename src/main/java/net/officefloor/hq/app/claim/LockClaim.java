package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Opens the claim's editor for the user: only a draft may be edited, and not while another user
 * has its editor open (on the sheet's today). Any other editor the user had open on the same
 * person's claims is released, as the person's form edits one claim at a time.
 */
public class LockClaim {
    public void lock(@Val Sheet sheet, @Val Claim claim, ClaimRepository repository,
            HttpServletRequest request) throws InvalidClaimStatusException, ClaimLockedException {
        claim.checkEditable();
        String user = Audit.user(request);
        if (!repository.lock(claim, user, sheet.today())) throw new ClaimLockedException(String.valueOf(claim.getId()));
        repository.unlockOthers(claim, user);
    }
}
