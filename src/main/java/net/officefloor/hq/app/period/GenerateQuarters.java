package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Makes the four calendar quarters of a year as periods named <yyyy>-Q1 to <yyyy>-Q4, each from the first
 *  day of its first month to the last day of its third, each recorded as PERIOD_ADDED. A quarter whose
 *  name is already a period, or whose days overlap a period on the sheet, is left out. A year that is not
 *  four digits is refused and nothing is written. */
public class GenerateQuarters {
    public void generate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody GenerateQuartersRequest request, SheetRepository sheets,
            PeriodRepository periods, Audit audit, Out<GeneratePeriodsResult> result) throws InvalidPeriodException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("A year is four digits (yyyy)");
        }
        int y = Integer.parseInt(year);
        sheets.open(id, user);
        int made = 0;
        for (int q = 1; q <= 4; q++) {
            String name = year + "-Q" + q;
            LocalDate first = YearMonth.of(y, q * 3 - 2).atDay(1);
            LocalDate last = YearMonth.of(y, q * 3).atEndOfMonth();
            if (periods.nameTaken(id, name) || periods.overlaps(id, first, last)) continue;
            Period period = periods.add(id, name, first, last, user);
            audit.record("PERIOD_ADDED sheet=" + id + " by=" + user + " period=" + period.name()
                    + " from=" + period.from() + " to=" + period.to());
            made++;
        }
        result.set(new GeneratePeriodsResult(made));
    }
}
