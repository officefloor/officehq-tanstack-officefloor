package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.claim.InvalidClaimStatusException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Refuses opening a claim whose period caps the open claims per person, when its person already
 * has that many OPEN or PENDING claims in it ({@link PeriodCaps}), before anything is written.
 */
public class CheckPeriodCap {
    public void check(@Val Sheet sheet, @Val Claim claim, PeriodRepository periods, ClaimRepository claims)
            throws InvalidClaimStatusException {
        Period full = PeriodCaps.full(sheet, claim, periods, claims);
        if (full != null) {
            throw new InvalidClaimStatusException("Period " + full.name() + " allows at most " + full.cap()
                    + " open claims per person");
        }
    }
}
