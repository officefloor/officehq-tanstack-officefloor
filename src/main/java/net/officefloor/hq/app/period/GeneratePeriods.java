package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds one period per calendar month from the from-date's month to the to-date's, each running
 *  from its month's first day to its last and named yyyy-mm, auditing each as PERIOD_ADDED. A month
 *  whose name is already a period, or whose days overlap one, is left out; the others are made. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId, @Val GeneratePeriodsRequest request,
            Periods periods, Audit audit, Out<GeneratePeriodsResult> result) {
        List<Period> existing = new ArrayList<>(periods.onSheet(sheetId));
        YearMonth last = YearMonth.from(LocalDate.parse(request.to()));
        int made = 0;
        for (YearMonth month = YearMonth.from(LocalDate.parse(request.from())); !month.isAfter(last);
                month = month.plusMonths(1)) {
            Period period = new Period(sheetId, month.toString(), month.atDay(1), month.atEndOfMonth());
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
