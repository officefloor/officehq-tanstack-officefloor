package net.officefloor.hq.app.patron;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a person named. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body) throws InvalidPatronException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPatronException("Sign in before naming patrons.");
        }
        if (request.patronId() == null) {
            throw new InvalidPatronException("Choose a person to name as a patron.");
        }
        body.set(new PatronRequest(request.user().trim(), request.patronId()));
    }
}
