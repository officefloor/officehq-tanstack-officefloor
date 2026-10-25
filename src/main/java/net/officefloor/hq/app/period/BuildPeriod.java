package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPeriod {
    public void build(@Val SheetResponse sheet, @Val PeriodInput input, @Val PeriodDates dates,
            PeriodRepository repository, Out<Period> period) {
        period.set(repository.insert(sheet.id(), input.name(), dates.from(), dates.to()));
    }
}
