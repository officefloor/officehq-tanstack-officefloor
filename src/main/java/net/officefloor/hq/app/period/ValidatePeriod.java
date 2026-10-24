package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the period form from the body, trimming the name; refuses an empty name, a from- or to-date that is
 *  not a calendar date, a to-date before the from-date, and a cap that is not blank or a whole number of at least 1. */
public class ValidatePeriod {

    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> body) throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A name is required.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must be on or after the from-date.");
        }
        String capText = request.cap() == null ? "" : request.cap().trim();
        Integer cap = null;
        if (!capText.isEmpty()) {
            if (!capText.matches("[0-9]{1,9}") || Integer.parseInt(capText) < 1) {
                throw new InvalidPeriodException("The cap must be a whole number of at least 1.");
            }
            cap = Integer.parseInt(capText);
        }
        body.set(new PeriodInput(name, from, to, cap));
    }

    private static LocalDate date(String value, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(value == null ? "" : value.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a calendar date.");
        }
    }
}
