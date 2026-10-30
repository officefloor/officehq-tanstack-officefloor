package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** A fiscal year's months: from the first day of the sheet's fiscal start month in the year, for twelve months. */
public class ValidateGenerateFiscalYear {

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GenerateFiscalYearRequest request,
            Out<ValidGenerate> body, Settings settings)
            throws InvalidPeriodException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String year = request.year() == null ? "" : request.year().trim();
        if (!year.matches("\\d{4}")) {
            throw new InvalidPeriodException("The fiscal year must be a year (YYYY)");
        }
        LocalDate from = LocalDate.of(Integer.parseInt(year), settings.fiscalStartMonth(sheetId), 1);
        body.set(new ValidGenerate(from, from.plusMonths(12).minusDays(1), request.user()));
    }
}
