package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds a period per calendar month from the from-date's month to the to-date's, each from the first to the last
 *  day of its month and named yyyy-mm, leaving out a month whose name the sheet already has or whose days overlap
 *  a period; refuses a date that is not a calendar date and a to-date before the from-date. */
public class GeneratePeriods {
    public void generate(@Val GeneratePeriodsRequest request, @Val Sheet sheet, PeriodRepository repository,
            Out<GeneratedPeriods> generatedOut) throws InvalidPeriodException {
        LocalDate today = sheet.getToday() != null ? sheet.getToday() : LocalDate.now();
        LocalDate from = date(request.from(), "from", today);
        LocalDate to = date(request.to(), "to", today);
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date.");
        }
        generatedOut.set(months(sheet, YearMonth.from(from), YearMonth.from(to), repository));
    }

    /** Adds a period per calendar month from the first month to the last, leaving out a month whose name the sheet
     *  already has or whose days overlap a period. */
    static GeneratedPeriods months(Sheet sheet, YearMonth firstMonth, YearMonth lastMonth, PeriodRepository repository) {
        List<Period> periods = new ArrayList<>(repository.findBySheetIdOrderByFromDateAscIdAsc(sheet.getId()));
        List<Period> made = new ArrayList<>();
        for (YearMonth month = firstMonth; !month.isAfter(lastMonth); month = month.plusMonths(1)) {
            String name = month.toString();
            LocalDate first = month.atDay(1);
            LocalDate last = month.atEndOfMonth();
            if (periods.stream().anyMatch(p -> p.getName().equals(name) || p.overlaps(first, last))) {
                continue;
            }
            Period period = repository.saveAndFlush(new Period(sheet.getId(), name, first, last));
            periods.add(period);
            made.add(period);
        }
        return new GeneratedPeriods(made);
    }

    private static LocalDate date(String value, String which, LocalDate today) throws InvalidPeriodException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return today;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a calendar date.");
        }
    }
}
