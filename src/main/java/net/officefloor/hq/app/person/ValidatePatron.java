package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the patron request; one without a patron is refused. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body) throws InvalidPersonException {
        if (request == null || request.patronId() == null) throw new InvalidPersonException("Choose a patron.");
        body.set(request);
    }
}
