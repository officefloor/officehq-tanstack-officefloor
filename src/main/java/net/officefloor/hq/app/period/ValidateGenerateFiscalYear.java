package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Turns a fiscal year (yyyy) into the range of its twelve months, from the sheet's fiscal start month in that year,
 *  for the period generator. */
public class ValidateGenerateFiscalYear {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody GenerateFiscalYearRequest request,
            Settings settings, Out<GeneratePeriodsInput> body) throws InvalidPeriodException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("Not a valid fiscal year: " + year);
        }
        YearMonth first = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheet));
        body.set(new GeneratePeriodsInput(first.atDay(1), first.plusMonths(11).atEndOfMonth()));
    }
}
