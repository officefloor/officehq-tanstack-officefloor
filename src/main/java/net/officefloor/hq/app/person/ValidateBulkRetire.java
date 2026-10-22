package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, at least one selected person, and the
 *  date, when given, a real yyyy-mm-dd date. */
public class ValidateBulkRetire {
    public void validate(@RequestBody BulkRetireRequest request, Out<BulkRetireInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before retiring people.");
        }
        if (request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidPersonException("Select the people to retire.");
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
        body.set(new BulkRetireInput(request.user().trim(), new LinkedHashSet<>(request.personIds()), from));
    }
}
