package net.officefloor.hq.app.period;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * One period per calendar month from the from-date's month to the to-date's month, each from the month's first to its
 * last day and named yyyy-mm; a month whose name is already a period on the sheet (ignoring case), or whose days
 * overlap one, is left out. A blank date is the sheet's today; a to-date before the from-date is refused.
 */
public class BuildGeneratedPeriods {
    public void build(@RequestBody @Valid GeneratePeriodsRequest request, @Val Sheet sheet, PeriodRepository repository,
            Out<GeneratedPeriods> generated) throws InvalidPeriodException {
        LocalDate from = date(request.from(), "from-date", sheet.today());
        LocalDate to = date(request.to(), "to-date", sheet.today());
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must not be before the from-date.");
        }
        List<Period> existing = repository.findAll(sheet.id());
        List<Period> made = new ArrayList<>();
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to)); month = month.plusMonths(1)) {
            Period period = new Period(sheet.id(), month.toString(), month.atDay(1), month.atEndOfMonth());
            boolean free = existing.stream()
                    .noneMatch((other) -> other.name().equalsIgnoreCase(period.name()) || other.overlaps(period));
            if (free) {
                made.add(period);
            }
        }
        generated.set(new GeneratedPeriods(List.copyOf(made)));
    }

    private static LocalDate date(String value, String label, LocalDate today) throws InvalidPeriodException {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty()) {
            return today;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + label + " must be yyyy-mm-dd.");
        }
    }
}
