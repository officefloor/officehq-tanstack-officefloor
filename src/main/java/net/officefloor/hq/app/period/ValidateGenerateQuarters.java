package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the year is yyyy. */
public class ValidateGenerateQuarters {
    public void validate(@RequestBody GenerateQuartersRequest request, Out<GenerateQuartersValues> values)
            throws InvalidPeriodException {
        String text = request == null || request.year() == null ? "" : request.year().trim();
        if (!text.matches("\\d{4}")) {
            throw new InvalidPeriodException("The year must be a year yyyy");
        }
        String by = request.by() == null ? "" : request.by().trim();
        values.set(new GenerateQuartersValues(Integer.parseInt(text), by));
    }
}
