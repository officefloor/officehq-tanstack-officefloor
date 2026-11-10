package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> body)
            throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) throw new InvalidPeriodException("A period's name is required");
        if (name.length() > 200) throw new InvalidPeriodException("A period's name is at most 200 characters");
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to == null) throw new InvalidPeriodException("A to-date is required");
        body.set(new PeriodInput(name, from, to));
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
