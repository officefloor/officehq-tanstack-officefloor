package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the year's four calendar quarters as periods named yyyy-Q1 to yyyy-Q4, each from the first day of its first
 *  month to the last day of its third, leaving out a quarter whose name the sheet already has or whose days overlap
 *  a period — as the period form would refuse it. */
public class GenerateQuarters {
    public void generate(@Val GenerateQuartersRequest request, @Val Sheet sheet, PeriodRepository repository,
            Out<GeneratedPeriods> generatedOut) {
        int year = Integer.parseInt(request.year());
        List<Period> periods = new ArrayList<>(repository.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()));
        List<Period> made = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            String name = year + "-Q" + quarter;
            LocalDate first = YearMonth.of(year, quarter * 3 - 2).atDay(1);
            LocalDate last = YearMonth.of(year, quarter * 3).atEndOfMonth();
            if (periods.stream().anyMatch(p -> p.getName().equals(name) || p.overlaps(first, last))) {
                continue;
            }
            Period period = repository.saveAndFlush(new Period(sheet.getId(), name, first, last));
            periods.add(period);
            made.add(period);
        }
        generatedOut.set(new GeneratedPeriods(made));
    }
}
