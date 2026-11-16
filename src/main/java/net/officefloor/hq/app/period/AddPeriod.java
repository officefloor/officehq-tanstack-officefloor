package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the new period on the sheet. */
public class AddPeriod {
    public void add(@PathVariable("sheetId") String sheetId, @Val PeriodValues values, PeriodRepository periods,
            Out<Period> period) {
        period.set(periods.add(sheetId.trim(), values.name(), values.from(), values.to()));
    }
}
