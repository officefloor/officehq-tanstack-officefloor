package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerateQuarters {
    public void validate(@RequestBody GenerateQuartersRequest request, Out<QuarterYear> quarterYear)
            throws InvalidPeriodException {
        String value = request == null || request.year() == null ? "" : request.year().trim();
        if (!value.matches("\\d{4}")) throw new InvalidPeriodException("A year is yyyy");
        quarterYear.set(new QuarterYear(Integer.parseInt(value)));
    }
}
