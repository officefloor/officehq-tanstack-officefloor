package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the patron body, refusing one that names no one. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronInput> patronOut) throws InvalidPersonException {
        if (request == null || request.patronId() == null) {
            throw new InvalidPersonException("Choose a patron.");
        }
        patronOut.set(new PatronInput(request.patronId()));
    }
}
