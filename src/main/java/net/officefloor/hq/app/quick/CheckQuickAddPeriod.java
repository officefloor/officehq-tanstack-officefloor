package net.officefloor.hq.app.quick;

import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Refuses a quick-add while the claim's date (the sheet's entry date while it is set, else its today) is inside a closed period, before anything is written. */
public class CheckQuickAddPeriod {
    public void check(@Val Sheet sheet, PeriodRepository periods, SettingsRepository settings) throws ClosedPeriodException {
        Period closed = periods.closedOn(sheet.id(), settings.find(sheet.id()).entryDay(sheet.today()));
        if (closed != null) throw new ClosedPeriodException(closed.name());
    }
}
