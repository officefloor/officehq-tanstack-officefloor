package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveToday {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate today,
            SheetClock clock, PeriodCloser closer, Rollup rollup, Claims claims, SheetStates states) {
        Rollup.Snapshot before = rollup.take(sheet);
        // A change of today releases every claim editor's lock on the sheet.
        LocalDate was = clock.today(sheet);
        if (!today.equals(was)) {
            claims.unlockAll(sheet);
            // What the sheet holds at the end of its old today is kept, for a statement taken as of that date.
            states.keep(sheet, was, today);
        }
        clock.setToday(sheet, today);
        // Periods past their grace days close before the figures the today change moved are audited.
        closer.autoClose(sheet, user);
        rollup.record(sheet, user, before);
    }
}
