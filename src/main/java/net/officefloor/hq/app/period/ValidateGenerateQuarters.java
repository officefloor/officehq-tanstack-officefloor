package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the generate-quarters form from the body; refuses a year that is not four digits. */
public class ValidateGenerateQuarters {

    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateQuartersRequest request, Out<GenerateQuartersRequest> body)
            throws InvalidPeriodException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("The year must be a year (yyyy).");
        }
        body.set(new GenerateQuartersRequest(year));
    }
}
