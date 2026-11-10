package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> body)
            throws InvalidPersonException {
        LocalDate from = date(request == null ? null : request.from(), "A hold's from-date is a date");
        LocalDate to = date(request == null ? null : request.to(), "A hold's to-date is a date");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPersonException("A hold's to-date is not before its from-date");
        }
        body.set(new HoldInput(from, to));
    }

    private static LocalDate date(String value, String message) throws InvalidPersonException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) return null;
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException(message);
        }
    }
}
