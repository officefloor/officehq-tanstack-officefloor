package net.officefloor.hq.app.period;

import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Accepts a calendar year (yyyy) for the quarter generator, refusing anything else. */
public class ValidateGenerateQuarters {
    private static final Pattern YEAR = Pattern.compile("\\d{4}");

    public void validate(@RequestBody GenerateQuartersRequest request, Out<GenerateQuartersInput> body)
            throws InvalidPeriodException {
        String year = request == null || request.year() == null ? "" : request.year().trim();
        if (!YEAR.matcher(year).matches()) {
            throw new InvalidPeriodException("Not a valid year: " + year);
        }
        body.set(new GenerateQuartersInput(Integer.parseInt(year)));
    }
}
