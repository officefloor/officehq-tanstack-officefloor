package net.officefloor.hq.app.rollover;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.period.FiscalYearPeriods;
import net.officefloor.hq.app.period.GeneratedPeriods;
import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Checks the rollover and builds the next fiscal year's twelve monthly periods, made as the fiscal year generator
 * makes them. Refused (400) when the year is not four digits, the user does not act as a steward, or no period on the
 * sheet starts in the old fiscal year.
 */
public class BuildRollover {
    public void build(@RequestBody @Valid RolloverRequest request, @RequestParam("user") String user, @Val Sheet sheet,
            PeriodRepository repository, SettingRepository settings, Out<Rollover> rollover,
            Out<GeneratedPeriods> generated) throws InvalidPeriodException {
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be yyyy.");
        }
        if (settings.find(sheet.id()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPeriodException("Only a steward may roll the year over.");
        }
        YearMonth oldFirst = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet.id()));
        YearMonth newFirst = oldFirst.plusYears(1);
        LocalDate start = oldFirst.atDay(1);
        LocalDate end = newFirst.atDay(1);
        List<Period> oldPeriods = repository.findAll(sheet.id()).stream()
                .filter((p) -> !p.from().isBefore(start) && p.from().isBefore(end))
                .sorted(Comparator.comparing(Period::to)).toList();
        if (oldPeriods.isEmpty()) {
            throw new InvalidPeriodException("The fiscal year FY" + year + " has no periods.");
        }
        rollover.set(new Rollover("FY" + year, oldPeriods, newFirst));
        generated.set(FiscalYearPeriods.of(sheet, repository, newFirst));
    }
}
