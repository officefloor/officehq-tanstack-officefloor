package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a claim added, or edited, to a date inside a closed period. */
public class CheckDatePeriod {
    public void check(@PathVariable("sheetId") String sheetId, @Val ClaimInput input, PeriodStore periods)
            throws InvalidSheetException {
        CheckClaimPeriod.refuseClosed(periods, sheetId.trim(), input.date());
    }
}
