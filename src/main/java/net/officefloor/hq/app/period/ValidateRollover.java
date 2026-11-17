package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the year is yyyy, the one rolling it over is a steward, and the fiscal year has periods (those
 *  whose from-date falls in it). Publishes the old year's periods, and the next fiscal year's dates for the period
 *  generator, as generating that fiscal year does (see ValidateGenerateFiscalYear). */
public class ValidateRollover {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverRequest request,
            SettingRepository settings, RoleRepository roles, PeriodRepository periods, Out<RolloverValues> rollover,
            Out<GeneratePeriodsValues> values) throws InvalidPeriodException {
        String sheet = sheetId.trim();
        String text = request == null || request.year() == null ? "" : request.year().trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (!text.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year yyyy");
        }
        if (!RoleRepository.STEWARD.equals(roles.roleOf(sheet, by))) {
            throw new InvalidPeriodException("Only a steward may roll a year over");
        }
        YearMonth first = YearMonth.of(Integer.parseInt(text), settings.fiscalStartMonth(sheet));
        LocalDate from = first.atDay(1);
        LocalDate to = first.plusMonths(11).atEndOfMonth();
        List<Period> old = periods.list(sheet).stream()
                .filter((p) -> !p.from().isBefore(from) && !p.from().isAfter(to))
                .sorted(Comparator.comparing(Period::from)).toList();
        if (old.isEmpty()) {
            throw new InvalidPeriodException("The fiscal year FY" + text + " has no periods");
        }
        rollover.set(new RolloverValues("FY" + text, old, by));
        YearMonth next = first.plusMonths(12);
        values.set(new GeneratePeriodsValues(next.atDay(1), next.plusMonths(11).atEndOfMonth(), by));
    }
}
