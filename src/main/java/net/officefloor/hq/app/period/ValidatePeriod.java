package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the name is not empty, the from and to dates are yyyy-MM-dd, and the to-date is not before the
 *  from-date; the cap, when given, is a whole number above 0. */
public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<PeriodValues> values) throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A period needs a name");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date may not be before the from-date");
        }
        values.set(new PeriodValues(name, from, to, request.by() == null ? "" : request.by().trim(), cap(request.cap())));
    }

    private static Integer cap(String text) throws InvalidPeriodException {
        String cap = text == null ? "" : text.trim();
        if (cap.isEmpty()) {
            return null;
        }
        if (!cap.matches("[0-9]{1,9}") || Integer.parseInt(cap) < 1) {
            throw new InvalidPeriodException("The cap must be a whole number above 0");
        }
        return Integer.parseInt(cap);
    }

    private static LocalDate date(String text, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date yyyy-MM-dd");
        }
    }
}
