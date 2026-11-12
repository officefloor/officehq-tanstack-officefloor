package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;

/** Refuses a change to a claim due by a date inside a closed period (the period a claim belongs to is the one its
 *  due-by falls in): editing it or moving it on. */
public class CheckClaimPeriod {
    public void check(@Val Claim claim, PeriodStore periods) throws InvalidSheetException {
        ClaimPeriods.refuseClosed(periods, claim.getSheetId(), claim.getDueBy());
    }
}
