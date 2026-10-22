package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, and the date, when
 *  given, a real yyyy-mm-dd date. */
public class ValidateRetire {
    public void validate(@RequestBody RetireRequest request, Out<RetireInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before retiring people.");
        }
        String raw = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!raw.isEmpty()) {
            if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new InvalidPersonException("The retire date must be a date (yyyy-mm-dd).");
            }
            try {
                from = LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The retire date must be a real date (yyyy-mm-dd).");
            }
        }
        body.set(new RetireInput(request.user().trim(), from));
    }
}
