package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Makes the four calendar quarters of a year, named yyyy-Q1 to yyyy-Q4, as the period form adds
 * them: a quarter whose name is already a period, or whose days overlap a period on the sheet, is
 * left out; each made is saved and audited in turn.
 */
public class GenerateQuarters {
    public void generate(@Val QuarterYear quarterYear, @Val Sheet sheet, PeriodRepository repository,
            HttpServletRequest request, Audit audit, Out<GeneratePeriodsResponse> result) {
        List<Period> existing = repository.findAll(sheet.id());
        int made = 0;
        for (int q = 1; q <= 4; q++) {
            LocalDate from = LocalDate.of(quarterYear.year(), q * 3 - 2, 1);
            Period period = new Period(sheet.id(), quarterYear.year() + "-Q" + q, from, from.plusMonths(3).minusDays(1));
            if (existing.stream().anyMatch(o -> o.name().equals(period.name()) || o.overlaps(period))) continue;
            repository.insert(period);
            audit.record("PERIOD_ADDED sheet=" + sheet.id() + " by=" + Audit.user(request)
                    + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
            made++;
        }
        result.set(new GeneratePeriodsResponse(made));
    }
}
