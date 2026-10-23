package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a name, from and to real yyyy-mm-dd dates,
 *  from on or before to; a cap blank or a whole number of at least 1. */
public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<PeriodInput> body) throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before adding periods.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 200) {
            throw new InvalidPeriodException("The period needs a name.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("The from-date must be on or before the to-date.");
        }
        String capText = request.cap() == null ? "" : request.cap().trim();
        Integer cap = null;
        if (!capText.isEmpty()) {
            if (!capText.matches("\\d{1,6}") || Integer.parseInt(capText) < 1) {
                throw new InvalidPeriodException("The cap must be a whole number of at least 1, or blank for none.");
            }
            cap = Integer.parseInt(capText);
        }
        body.set(new PeriodInput(request.user().trim(), name, from, to, cap));
    }

    private static LocalDate date(String text, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date, yyyy-mm-dd.");
        }
    }
}
