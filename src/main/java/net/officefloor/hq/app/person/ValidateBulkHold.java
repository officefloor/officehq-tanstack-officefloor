package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, at least one selected person, and the
 *  from- and to-dates, when given, real yyyy-mm-dd dates, the to-date not before the from-date. */
public class ValidateBulkHold {
    public void validate(@RequestBody BulkHoldRequest request, Out<BulkHoldInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before holding people.");
        }
        if (request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidPersonException("Select the people to hold.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidPersonException("The hold's to-date must not be before its from-date.");
        }
        body.set(new BulkHoldInput(request.user().trim(), new LinkedHashSet<>(request.personIds()), from, to));
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
