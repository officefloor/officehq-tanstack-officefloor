package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the generate-fiscal-year form from the body; refuses a year that is not four digits. */
public class ValidateGenerateFiscalYear {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateFiscalYearRequest request, Out<GenerateFiscalYearRequest> body)
            throws InvalidPeriodException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("The fiscal year must be a year (yyyy).");
        }
        body.set(new GenerateFiscalYearRequest(year));
    }
}
