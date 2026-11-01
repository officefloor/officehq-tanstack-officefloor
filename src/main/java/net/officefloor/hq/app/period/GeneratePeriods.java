package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds one period per calendar month from the from-date's month to the to-date's, named yyyy-mm, leaving out a month
 *  whose name is already a period or whose days overlap one; publishes how many were made. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val GeneratePeriodsInput input, Periods periods, Audit audit, Out<Integer> made) {
        int count = 0;
        YearMonth last = YearMonth.from(input.to());
        for (YearMonth month = YearMonth.from(input.from()); !month.isAfter(last); month = month.plusMonths(1)) {
            PeriodInput period = new PeriodInput(month.toString(), month.atDay(1), month.atEndOfMonth());
            if (periods.named(sheet, period.name()) || periods.overlaps(sheet, period)) {
                continue;
            }
            periods.add(sheet, period);
            audit.record("PERIOD_ADDED sheet=" + sheet + " by=" + user + " period=" + period.name() + " from="
                    + period.from() + " to=" + period.to());
            count++;
        }
        made.set(count);
    }
}
