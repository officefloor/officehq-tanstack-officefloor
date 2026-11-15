package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Opens a claim's editor: locks it for the user until the editor is saved or cancelled, or the
 *  sheet's today changes. Refused, taking no lock, for a claim that cannot be edited (not a DRAFT, or
 *  due by a day in a closed period) and for one whose lock another user holds. */
public class LockClaim {
    public void lock(@RequestParam(name = "by", required = false) String by, @Val Claim claim,
            PeriodRepository periods, ClaimRepository claims)
            throws ClaimStatusException, PeriodClosedException, ClaimLockedException {
        String user = by == null ? "" : by;
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be edited");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (claim.getLockedBy() != null && !claim.getLockedBy().equals(user)) {
            throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy());
        }
        claims.lock(claim, user);
    }
}
