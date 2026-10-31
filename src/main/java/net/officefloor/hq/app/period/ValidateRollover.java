package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A rollover is by a steward only, of a year (YYYY) whose fiscal year has at least one period on the sheet (a
 *  period belongs to the fiscal year its from-date falls in). */
public class ValidateRollover {

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverRequest request,
            Out<ValidRollover> body, Periods periods, Settings settings)
            throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        if (!Settings.STEWARD.equals(settings.role(sheetId, request.user()))) {
            throw new InvalidPeriodException("Only a steward may roll the year over");
        }
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year (YYYY)");
        }
        LocalDate from = LocalDate.of(Integer.parseInt(year), settings.fiscalStartMonth(sheetId), 1);
        LocalDate next = from.plusMonths(12);
        List<PeriodResponse> inYear = periods.list(sheetId).stream()
                .filter(p -> !LocalDate.parse(p.from()).isBefore(from) && LocalDate.parse(p.from()).isBefore(next))
                .toList();
        if (inYear.isEmpty()) {
            throw new InvalidPeriodException("No period of FY" + year + " is on the sheet");
        }
        body.set(new ValidRollover("FY" + year, inYear,
                new ValidGenerate(next, next.plusMonths(12).minusDays(1), request.user()), request.user()));
    }
}
