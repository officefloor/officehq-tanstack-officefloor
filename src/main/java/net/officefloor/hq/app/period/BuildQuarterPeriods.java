package net.officefloor.hq.app.period;

import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * The four calendar quarters of the year typed, named yyyy-Q1 to yyyy-Q4, each from its first month's first day to
 * its last month's last day; a quarter whose name is already a period on the sheet (ignoring case), or whose days
 * overlap one, is left out. A year that is not four digits is refused.
 */
public class BuildQuarterPeriods {
    public void build(@RequestBody @Valid GenerateQuartersRequest request, @Val Sheet sheet,
            PeriodRepository repository, Out<GeneratedPeriods> generated) throws InvalidPeriodException {
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The year must be yyyy.");
        }
        List<Period> existing = repository.findAll(sheet.id());
        List<Period> made = new ArrayList<>();
        for (int quarter = 1; quarter <= 4; quarter++) {
            YearMonth first = YearMonth.of(Integer.parseInt(year), quarter * 3 - 2);
            Period period = new Period(sheet.id(), year + "-Q" + quarter, first.atDay(1),
                    first.plusMonths(2).atEndOfMonth());
            boolean free = existing.stream()
                    .noneMatch((other) -> other.name().equalsIgnoreCase(period.name()) || other.overlaps(period));
            if (free) {
                made.add(period);
            }
        }
        generated.set(new GeneratedPeriods(List.copyOf(made)));
    }
}
