package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Makes one period per calendar month from the from-date's month to the to-date's month (a blank
 * from-date is the sheet's today), each running from the first to the last day of its month and
 * named yyyy-mm. A to-date before the from-date is refused. A month whose name is already a period,
 * or whose days overlap a period on the sheet, is left out; each made is saved and audited in turn.
 */
public class GeneratePeriods {
    public void generate(@Val GeneratePeriodsInput input, @Val Sheet sheet, PeriodRepository repository,
            HttpServletRequest request, Audit audit, Out<GeneratePeriodsResponse> result)
            throws InvalidPeriodException {
        LocalDate from = input.from() == null ? sheet.today() : input.from();
        if (input.to().isBefore(from)) {
            throw new InvalidPeriodException("A to-date is on or after the from-date");
        }
        List<Period> existing = repository.findAll(sheet.id());
        int made = 0;
        for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(input.to())); month = month.plusMonths(1)) {
            Period period = new Period(sheet.id(), month.toString(), month.atDay(1), month.atEndOfMonth());
            if (existing.stream().anyMatch(o -> o.name().equals(period.name()) || o.overlaps(period))) continue;
            repository.insert(period);
            audit.record("PERIOD_ADDED sheet=" + sheet.id() + " by=" + Audit.user(request)
                    + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            made++;
        }
        result.set(new GeneratePeriodsResponse(made));
    }
}
