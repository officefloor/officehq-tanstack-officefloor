package net.officefloor.hq.app.rollover;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import net.officefloor.hq.app.period.InvalidPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodGenerateInput;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a rollover by anyone but a steward, or of a fiscal year with no periods on the sheet (a period belongs to the
 *  fiscal year its from-date falls in); otherwise publish the old year and the next fiscal year's twelve months to
 *  generate. */
public class CheckRollover {
    public void check(@PathVariable("sheetId") String sheetId, @Val RolloverInput input, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, Out<OldYear> oldYear, Out<PeriodGenerateInput> generate)
            throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (!Role.STEWARD.equals(roles.of(id, input.user()))) {
            throw new InvalidPeriodException("Only a steward may roll the year over on this sheet.");
        }
        YearMonth first = YearMonth.of(input.year(), settings.fiscalStartMonth(id));
        LocalDate start = first.atDay(1);
        LocalDate next = first.plusMonths(12).atDay(1);
        List<Period> inYear = periods.list(id).stream()
                .filter(p -> !p.from().isBefore(start) && p.from().isBefore(next)).toList();
        String name = "FY" + input.year();
        if (inYear.isEmpty()) {
            throw new InvalidPeriodException(name + " has no periods on this sheet to roll over.");
        }
        oldYear.set(new OldYear(name, inYear));
        YearMonth newFirst = first.plusMonths(12);
        generate.set(new PeriodGenerateInput(input.user(), newFirst, newFirst.plusMonths(11)));
    }
}
