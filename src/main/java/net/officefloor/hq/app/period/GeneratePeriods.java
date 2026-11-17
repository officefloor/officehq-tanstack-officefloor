package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores one period per calendar month from the from-date's month to the to-date's month, each running from the
 *  first to the last day of its month and named yyyy-mm, and writes each as PERIOD_ADDED. A month whose name is
 *  already a period of the sheet, or whose days overlap one, is left out. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GeneratePeriodsValues values,
            PeriodRepository periods, Audit audit, Out<GeneratedPeriods> generated) {
        String sheet = sheetId.trim();
        int made = 0;
        java.util.List<Long> ids = new java.util.ArrayList<>();
        YearMonth last = YearMonth.from(values.to());
        for (YearMonth month = YearMonth.from(values.from()); !month.isAfter(last); month = month.plusMonths(1)) {
            String name = month.toString();
            if (periods.nameTaken(sheet, name) || periods.overlaps(sheet, month.atDay(1), month.atEndOfMonth())) {
                continue;
            }
            Period period = periods.add(sheet, name, month.atDay(1), month.atEndOfMonth(), null);
            audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + values.by() + " period=" + period.name()
                    + " from=" + period.from() + " to=" + period.to());
            ids.add(period.id());
            made++;
        }
        generated.set(new GeneratedPeriods(made, ids));
    }
}
