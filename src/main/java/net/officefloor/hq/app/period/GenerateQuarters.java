package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the four calendar quarters of the year as periods named yyyy-Q1 to yyyy-Q4, each running from the first day
 *  of its first month to the last day of its third, and writes each as PERIOD_ADDED. A quarter whose name is already
 *  a period of the sheet, or whose days overlap one, is left out. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GenerateQuartersValues values,
            PeriodRepository periods, Audit audit, Out<GeneratedPeriods> generated) {
        String sheet = sheetId.trim();
        int made = 0;
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            YearMonth first = YearMonth.of(values.year(), quarter * 3 - 2);
            LocalDate from = first.atDay(1);
            LocalDate to = first.plusMonths(2).atEndOfMonth();
            String name = values.year() + "-Q" + quarter;
            if (periods.nameTaken(sheet, name) || periods.overlaps(sheet, from, to)) {
                continue;
            }
            Period period = periods.add(sheet, name, from, to, null);
            audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + values.by() + " period=" + period.name()
                    + " from=" + period.from() + " to=" + period.to());
            ids.add(period.id());
            made++;
        }
        generated.set(new GeneratedPeriods(made, ids));
    }
}
