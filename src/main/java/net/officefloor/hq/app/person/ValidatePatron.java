package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Binds the patron asked for: refused when none is named. */
public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body)
            throws InvalidPersonException {
        if (request == null || request.patronId() == null) {
            throw new InvalidPersonException("No patron named");
        }
        body.set(request);
    }
}
