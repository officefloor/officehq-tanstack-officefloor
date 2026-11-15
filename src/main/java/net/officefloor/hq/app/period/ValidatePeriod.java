package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePeriod {
    public void validate(@RequestBody PeriodRequest request, Out<ValidPeriod> body)
            throws InvalidPeriodException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidPeriodException("A name is required");
        }
        if (name.length() > 200) {
            throw new InvalidPeriodException("The name is too long");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from.isAfter(to)) {
            throw new InvalidPeriodException("The from-date must be on or before the to-date");
        }
        body.set(new ValidPeriod(name, from, to, cap(request.cap())));
    }

    private static Integer cap(String text) throws InvalidPeriodException {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) return null;
        if (!t.matches("[0-9]{1,9}") || Integer.parseInt(t) < 1) {
            throw new InvalidPeriodException("The cap must be a whole number above zero");
        }
        return Integer.parseInt(t);
    }

    private static LocalDate date(String text) throws InvalidPeriodException {
        String t = text == null ? "" : text.trim();
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("Not a date: " + t);
        }
    }
}
