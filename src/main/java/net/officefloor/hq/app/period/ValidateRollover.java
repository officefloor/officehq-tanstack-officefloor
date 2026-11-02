package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Checks a year end rollover: only a steward may, of a fiscal year (yyyy) that has periods on the sheet (one whose
 * from-date falls in it). Publishes the old year and, for the period generator, the next fiscal year's twelve months.
 */
public class ValidateRollover {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody RolloverRequest request, Roles roles, Settings settings, Periods periods,
            Out<RolloverYear> rollover, Out<GeneratePeriodsInput> body) throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPeriodException("Only a steward may roll the year over");
        }
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("Not a valid fiscal year: " + year);
        }
        YearMonth first = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        LocalDate from = first.atDay(1);
        LocalDate to = first.plusMonths(11).atEndOfMonth();
        boolean any = periods.list(sheet).stream().map((p) -> LocalDate.parse(p.from()))
                .anyMatch((d) -> !d.isBefore(from) && !d.isAfter(to));
        if (!any) {
            throw new InvalidPeriodException("The fiscal year FY" + year + " has no periods");
        }
        YearMonth next = first.plusMonths(12);
        rollover.set(new RolloverYear("FY" + year, from, to, next.atDay(1)));
        body.set(new GeneratePeriodsInput(next.atDay(1), next.plusMonths(11).atEndOfMonth()));
    }
}
