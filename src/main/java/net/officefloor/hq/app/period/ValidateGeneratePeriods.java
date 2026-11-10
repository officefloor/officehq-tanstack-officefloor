package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGeneratePeriods {
    public void validate(@RequestBody GeneratePeriodsRequest request, Out<GeneratePeriodsInput> body)
            throws InvalidPeriodException {
        LocalDate from = date(request == null ? null : request.from(), "from");
        LocalDate to = date(request == null ? null : request.to(), "to");
        if (to == null) throw new InvalidPeriodException("A to-date is required");
        body.set(new GeneratePeriodsInput(from, to));
    }

    private static LocalDate date(String raw, String which) throws InvalidPeriodException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("A " + which + "-date is a date, yyyy-MM-dd");
        }
    }
}
