package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user. Publishes the user trimmed. */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<StatementRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before taking a statement.");
        }
        body.set(new StatementRequest(request.user().trim()));
    }
}
