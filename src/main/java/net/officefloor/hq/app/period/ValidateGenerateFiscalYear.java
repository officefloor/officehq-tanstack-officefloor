package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the year is yyyy. Publishes the fiscal year's dates for the period generator: from the first day
 *  of the sheet's fiscal start month in that year to the last day of the eleventh month after it. */
public class ValidateGenerateFiscalYear {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest request,
            SettingRepository settings, Out<GeneratePeriodsValues> values) throws InvalidPeriodException {
        String text = request == null || request.year() == null ? "" : request.year().trim();
        if (!text.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year yyyy");
        }
        YearMonth first = YearMonth.of(Integer.parseInt(text), settings.fiscalStartMonth(sheetId.trim()));
        LocalDate from = first.atDay(1);
        LocalDate to = first.plusMonths(11).atEndOfMonth();
        String by = request.by() == null ? "" : request.by().trim();
        values.set(new GeneratePeriodsValues(from, to, by));
    }
}
