package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the four calendar quarters of the year as periods named yyyy-Q1 to yyyy-Q4, each running
 *  from its quarter's first day to its last, auditing each as PERIOD_ADDED as the period form does.
 *  A quarter whose name is already a period, or whose days overlap one, is left out. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GenerateQuartersRequest request,
            Periods periods, Audit audit, Out<GeneratePeriodsResult> result) {
        List<Period> existing = new ArrayList<>(periods.onSheet(sheetId));
        int year = Integer.parseInt(request.year());
        int made = 0;
        for (int q = 1; q <= 4; q++) {
            YearMonth first = YearMonth.of(year, q * 3 - 2);
            Period period = new Period(sheetId, request.year() + "-Q" + q, first.atDay(1),
                    first.plusMonths(2).atEndOfMonth());
            if (existing.stream().anyMatch(p -> p.name().equals(period.name()) || p.overlaps(period))) {
                continue;
            }
            periods.add(period);
            existing.add(period);
            audit.record("PERIOD_ADDED sheet=" + sheetId + " by=" + request.user()
                    + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            made++;
        }
        result.set(new GeneratePeriodsResult(made));
    }
}
