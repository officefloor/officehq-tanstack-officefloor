package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveToday {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate today,
            SheetClock clock, PeriodCloser closer, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        clock.setToday(sheet, today);
        // Periods past their grace days close before the figures the today change moved are audited.
        closer.autoClose(sheet, user);
        rollup.record(sheet, user, before);
    }
}
