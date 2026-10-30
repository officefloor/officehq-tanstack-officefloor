package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.Periods;
import net.officefloor.plugin.variable.Val;

/** Opens the claim's editor for the user: only a DRAFT claim outside any closed period, not locked by another user. */
public class ApplyLock {
    public void apply(@Val LockRequest request, @Val Claim claim, Periods periods, Claims claims) throws ClaimStatusRefusedException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new ClaimStatusRefusedException("Only a DRAFT claim can be edited");
        }
        if (periods.isClosed(claim.getSheetId(), claim.getDate())) {
            throw new ClaimStatusRefusedException("No claim dated inside a closed period can be edited");
        }
        String lockedBy = claims.lockedBy(claim);
        if (lockedBy != null && !lockedBy.equals(request.user())) {
            throw new ClaimStatusRefusedException("The claim is being edited by " + lockedBy);
        }
    }
}
