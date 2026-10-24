package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses the quick-add before anyone is added when its claim, dated the sheet's today, falls in a closed
 *  period — so a refused claim leaves no new person behind. */
public class CheckQuickAddDate {
    public void check(@Val Sheet sheet, PeriodRepository periods) throws InvalidClaimException {
        if (periods.isClosedOn(sheet.getId(), sheet.getToday())) {
            throw new InvalidClaimException("No claim can be added in a closed period.");
        }
    }
}
