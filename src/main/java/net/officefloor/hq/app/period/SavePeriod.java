package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SavePeriod {
    public void save(@PathVariable("sheetId") String sheetId, @Val PeriodInput period, PeriodStore store) {
        store.add(sheetId.trim(), period);
    }
}
