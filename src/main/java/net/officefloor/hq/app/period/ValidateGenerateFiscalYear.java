package net.officefloor.hq.app.period;

import java.time.YearMonth;
import java.util.regex.Pattern;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A fiscal year generate needs a year (yyyy). Fiscal year FY<yyyy> runs twelve months from the
 *  sheet's fiscal start month of yyyy; it is published as the generate of those months, so the
 *  months are made exactly as the period generator makes them. */
public class ValidateGenerateFiscalYear {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest request,
            Settings settings, Out<GeneratePeriodsRequest> body) throws InvalidSheetRequestException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidSheetRequestException("A fiscal year must be a year (yyyy)");
        }
        YearMonth first = YearMonth.of(Integer.parseInt(year), settings.fiscalStartMonth(sheetId));
        YearMonth last = first.plusMonths(11);
        body.set(new GeneratePeriodsRequest(first.atDay(1).toString(), last.atEndOfMonth().toString(), request.user()));
    }
}
