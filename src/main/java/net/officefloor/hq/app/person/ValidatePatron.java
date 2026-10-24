package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads from the body the id of the person to name as a patron. */
public class ValidatePatron {

    public record PatronRequest(Long patronId) {
    }

    public void validate(@RequestBody(required = false) PatronRequest request, Out<PatronInput> body)
            throws InvalidPersonException {
        if (request == null || request.patronId() == null) {
            throw new InvalidPersonException("Choose a patron.");
        }
        body.set(new PatronInput(request.patronId()));
    }
}
