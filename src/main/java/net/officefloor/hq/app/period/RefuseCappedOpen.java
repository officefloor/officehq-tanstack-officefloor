package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.ClaimStatusInput;
import net.officefloor.plugin.variable.Val;

/** Refuse opening a claim whose person already has as many OPEN or PENDING claims in its period as the period's cap. */
public class RefuseCappedOpen {
    public void refuse(@Val ClaimStatusInput input, @Val Claim loaded, PeriodRepository periods, ClaimRepository claims)
            throws CappedPeriodException {
        if (!input.action().equals("open")) {
            return;
        }
        Period capped = PeriodCap.reached(loaded, periods, claims);
        if (capped != null) {
            throw new CappedPeriodException(capped);
        }
    }
}
