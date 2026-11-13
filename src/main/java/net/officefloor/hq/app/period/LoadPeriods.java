package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class LoadPeriods {
    public void load(@Val Sheet sheet, PeriodRepository repository, Out<List<Period>> periods) {
        periods.set(repository.findAll(sheet.id()));
    }
}
