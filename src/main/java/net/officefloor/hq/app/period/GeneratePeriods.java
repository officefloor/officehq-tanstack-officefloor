package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Makes one period per calendar month from the from-date's month to the to-date's month, each from
 *  the first to the last day of its month and named yyyy-MM, each recorded as PERIOD_ADDED. A month
 *  whose name is already a period, or whose days overlap a period on the sheet, is left out. A
 *  to-date before the from-date is refused and nothing is written. */
public class GeneratePeriods {
    public void generate(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by,
            @RequestBody GeneratePeriodsRequest request, SheetRepository sheets, PeriodRepository periods,
            Audit audit, Out<GeneratePeriodsResult> result) throws InvalidPeriodException {
        String id = sheetId.trim();
        String user = by == null ? "" : by;
        LocalDate today = sheets.today(id);
        LocalDate from = date(request == null ? null : request.from(), today);
        LocalDate to = date(request == null ? null : request.to(), today);
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date");
        }
        sheets.open(id, user);
        int made = 0;
        for (YearMonth m = YearMonth.from(from); !m.isAfter(YearMonth.from(to)); m = m.plusMonths(1)) {
            String name = m.toString();
            LocalDate first = m.atDay(1);
            LocalDate last = m.atEndOfMonth();
            if (periods.nameTaken(id, name) || periods.overlaps(id, first, last)) continue;
            Period period = periods.add(id, name, first, last, user);
            audit.record("PERIOD_ADDED sheet=" + id + " by=" + user + " period=" + period.name()
                    + " from=" + period.from() + " to=" + period.to());
            made++;
        }
        result.set(new GeneratePeriodsResult(made));
    }

    private static LocalDate date(String text, LocalDate blank) throws InvalidPeriodException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return blank;
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Not a date: " + t);
        }
    }
}
