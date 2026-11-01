package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A rollover needs a year (yyyy), a steward, and a fiscal year with at least one period (a period
 *  is in the fiscal year its from-date falls in). Publishes the old year and its periods, and the
 *  generate of the next fiscal year's twelve months, made exactly as the fiscal year generator makes
 *  them. */
public class ValidateRollover {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RolloverRequest request,
            Settings settings, Periods periods, Out<RolloverYear> old, Out<GeneratePeriodsRequest> body)
            throws InvalidSheetRequestException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        String user = request == null ? null : request.user();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidSheetRequestException("A fiscal year must be a year (yyyy)");
        }
        if (settings.role(sheetId, user) != Role.STEWARD) {
            throw new InvalidSheetRequestException("Only a steward may roll the year over");
        }
        int fy = Integer.parseInt(year);
        int startMonth = settings.fiscalStartMonth(sheetId);
        List<Period> inYear = periods.onSheet(sheetId).stream()
                .filter(p -> fiscalYear(p, startMonth) == fy).toList();
        if (inYear.isEmpty()) {
            throw new InvalidSheetRequestException("FY" + fy + " has no periods to roll over");
        }
        old.set(new RolloverYear(fy, inYear, user));
        YearMonth first = YearMonth.of(fy + 1, startMonth);
        YearMonth last = first.plusMonths(11);
        body.set(new GeneratePeriodsRequest(first.atDay(1).toString(), last.atEndOfMonth().toString(), user));
    }

    /** The fiscal year (yyyy) the period's from-date falls in. */
    static int fiscalYear(Period period, int startMonth) {
        return period.from().getMonthValue() >= startMonth ? period.from().getYear() : period.from().getYear() - 1;
    }
}
