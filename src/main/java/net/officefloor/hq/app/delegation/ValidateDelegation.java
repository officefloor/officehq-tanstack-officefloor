package net.officefloor.hq.app.delegation;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a user name, and from- and
 *  to-dates that are real yyyy-mm-dd dates, the to-date not before the from-date. */
public class ValidateDelegation {
    public void validate(@RequestBody DelegationRequest request, Out<DelegationInput> body)
            throws InvalidDelegationException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidDelegationException("Sign in before delegating.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new InvalidDelegationException("Give the user name the steward role is delegated to.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidDelegationException("The delegation's to-date must not be before its from-date.");
        }
        body.set(new DelegationInput(request.user().trim(), name, from, to));
    }

    private static LocalDate date(String value, String which) throws InvalidDelegationException {
        String raw = value == null ? "" : value.trim();
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidDelegationException("The delegation's " + which + "-date must be a date (yyyy-mm-dd).");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidDelegationException("The delegation's " + which + "-date must be a real date (yyyy-mm-dd).");
        }
    }
}
