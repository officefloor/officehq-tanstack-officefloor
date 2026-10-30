package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronRequest> body) throws InvalidPersonException {
        if (request.patronId() == null || request.patronId().isBlank()) {
            throw new InvalidPersonException("No patron chosen");
        }
        body.set(request);
    }
}
