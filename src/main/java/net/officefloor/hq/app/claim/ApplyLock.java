package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Checks the claim's editor may be opened: only a DRAFT claim not whose due-by falls inside a closed period can be edited. */
public class ApplyLock {
    public void apply(@Val Claim claim, PeriodRepository periods) throws InvalidClaimStatusException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimStatusException("Only a DRAFT claim can be edited");
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy())) {
            throw new InvalidClaimStatusException("The claim's due-by is inside a closed period");
        }
    }
}
