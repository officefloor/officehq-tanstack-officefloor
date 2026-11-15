package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Deletes a DRAFT claim: it becomes DELETED and counts nowhere. Refused for any other status, and for
 *  a claim due by a day inside a closed period. */
public class DeleteClaim {
    public void move(@Val Claim claim, PeriodRepository periods) throws ClaimStatusException, PeriodClosedException {
        ClaimStatus deleted = ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.DELETED);
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        claim.setStatus(deleted.name());
    }
}
