package net.officefloor.hq.app.period;

import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds the four calendar quarters of the year as periods named yyyy-Q1 to yyyy-Q4, leaving out a quarter whose name
 *  is already a period or whose days overlap one; publishes how many were made. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val GenerateQuartersInput input, Periods periods, Audit audit, Out<Integer> made) {
        int count = 0;
        for (int quarter = 1; quarter <= 4; quarter++) {
            YearMonth first = YearMonth.of(input.year(), quarter * 3 - 2);
            PeriodInput period = new PeriodInput(input.year() + "-Q" + quarter, first.atDay(1),
                    first.plusMonths(2).atEndOfMonth());
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
