package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

public class VoidClaim {
    public void move(@Val Claim claim, PeriodRepository periods)
            throws ClaimStatusException, PeriodClosedException {
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        claim.setStatus(ClaimStatus.valueOf(claim.getStatus()).moveTo(ClaimStatus.VOID).name());
        claim.clearNumber();
    }
}
