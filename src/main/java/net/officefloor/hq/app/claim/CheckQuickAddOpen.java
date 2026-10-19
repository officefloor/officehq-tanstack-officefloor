package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a quick-add when the sheet's today falls inside a closed period, before any person is added. */
public class CheckQuickAddOpen {
    public void check(@PathVariable("sheetId") String sheetId, SheetClock clock, PeriodRepository periods)
            throws InvalidClaimException {
        ClosedPeriods.refuse(periods, sheetId, clock.today(sheetId));
    }
}
