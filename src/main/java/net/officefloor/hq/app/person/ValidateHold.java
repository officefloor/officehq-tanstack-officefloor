package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, and the from- and to-dates,
 *  when given, real yyyy-mm-dd dates, the to-date not before the from-date. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before holding people.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date.");
        }
        body.set(new HoldInput(request.user().trim(), from, to));
    }

    private static LocalDate date(String value, String which) throws InvalidPersonException {
        String raw = value == null ? "" : value.trim();
        if (raw.isEmpty()) {
            return null;
        }
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidPersonException("The hold's " + which + "-date must be a date (yyyy-mm-dd).");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPersonException("The hold's " + which + "-date must be a real date (yyyy-mm-dd).");
        }
    }
}
