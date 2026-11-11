package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerateFiscalYear {
    public void validate(@RequestBody GenerateFiscalYearRequest request, Out<FiscalYear> fiscalYear)
            throws InvalidPeriodException {
        String value = request == null || request.year() == null ? "" : request.year().trim();
        if (!value.matches("\\d{4}")) throw new InvalidPeriodException("A fiscal year is a year, yyyy");
        fiscalYear.set(new FiscalYear(Integer.parseInt(value)));
    }
}
