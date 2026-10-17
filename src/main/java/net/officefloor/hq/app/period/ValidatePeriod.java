package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the period from the body, refusing an empty name, a from or to that is not a date, or a to before from. */
public class ValidatePeriod {

    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> periodOut) throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("The period must have a name.");
        }
        if (name.length() > 100) {
            throw new InvalidPeriodException("The period's name must be at most 100 characters.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The period's to-date must be on or after its from-date.");
        }
        periodOut.set(new PeriodInput(name, from, to));
    }

    private static LocalDate date(String value, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The period's " + which + "-date must be a date (yyyy-mm-dd).");
        }
    }
}
