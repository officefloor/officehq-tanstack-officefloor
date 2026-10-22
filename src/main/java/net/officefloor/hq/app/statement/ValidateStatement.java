package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, and an as-of date that is blank or a date.
 *  Publishes the user and the as-of date trimmed (null: blank). */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before taking a statement.");
        }
        String asOf = request.asOf() == null || request.asOf().isBlank() ? null : request.asOf().trim();
        if (asOf != null) {
            try {
                LocalDate.parse(asOf);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("A statement's as-of date must be a date (YYYY-MM-DD).");
            }
        }
        body.set(new StatementRequest(request.user().trim(), asOf));
    }
}
