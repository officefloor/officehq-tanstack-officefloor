package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerateFiscalYear {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    // A year (yyyy): fiscal year FY<yyyy> starts on the first of the sheet's fiscal start month in that year and
    // runs twelve months. Each month becomes a period as the period generator makes it.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest body,
            Periods periods, Settings settings, Out<GeneratedPeriods> generatedOut) throws InvalidPeriodException {
        String year = body == null || body.year() == null ? "" : body.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("The fiscal year must be a year (yyyy).");
        }
        String sheet = sheetId.trim();
        YearMonth first = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        generatedOut.set(GeneratedPeriods.months(sheet, periods.list(sheet), first, first.plusMonths(11)));
    }
}
