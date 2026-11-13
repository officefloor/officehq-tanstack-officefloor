package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Checks a rollover: by a steward, of a year (YYYY) with periods on the sheet (a period belongs to the fiscal
 *  year its from-date falls in). Publishes the next fiscal year's twelve months to generate, as
 *  {@link ValidateGenerateFiscalYear} does. */
public class ValidateRollover {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverRequest request,
            SettingStore settings, PeriodStore periods, RoleStore roles, Out<RolloverInput> rollover,
            Out<GenerateInput> body) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        if (roles.find(sheet, user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may roll the year over");
        }
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidSheetException("The fiscal year must be a year (YYYY)");
        }
        YearMonth start = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        LocalDate first = start.atDay(1);
        LocalDate next = start.plusYears(1).atDay(1);
        List<PeriodResponse> old = periods.list(sheet).stream().filter(p -> {
            LocalDate from = LocalDate.parse(p.from());
            return !from.isBefore(first) && from.isBefore(next);
        }).toList();
        if (old.isEmpty()) {
            throw new InvalidSheetException("FY" + year + " has no periods");
        }
        YearMonth from = start.plusYears(1);
        rollover.set(new RolloverInput(start.getYear(), old, from, from.plusMonths(11), user,
                periods.list(sheet).stream().map(PeriodResponse::name).collect(Collectors.toSet())));
        body.set(new GenerateInput(from, from.plusMonths(11), user));
    }
}
