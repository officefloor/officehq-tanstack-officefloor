package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a claim dated in a closed period before anything is saved, so no new person is left behind. */
public class RefuseClosedQuickAdd {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val ClaimValues values, Sheets sheets,
            ClosedPeriods closedPeriods) throws PeriodClosedException {
        closedPeriods.refuse(sheetId, values.date() != null ? values.date() : sheets.today(sheetId));
    }
}
