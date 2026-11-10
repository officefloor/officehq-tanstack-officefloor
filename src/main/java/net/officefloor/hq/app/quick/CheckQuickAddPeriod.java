package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a quick-add while the sheet's today, the claim's date, is inside a closed period, before anything is written. */
public class CheckQuickAddPeriod {
    public void check(@Val Sheet sheet, PeriodRepository periods) throws ClosedPeriodException {
        Period closed = periods.closedOn(sheet.id(), sheet.today());
        if (closed != null) throw new ClosedPeriodException(closed.name());
    }
}
