package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidatePatron {
    public void validate(@RequestBody PatronRequest request, Out<PatronInput> body)
            throws InvalidPersonException {
        String raw = request == null || request.patronId() == null ? "" : request.patronId().trim();
        try {
            body.set(new PatronInput(Long.parseLong(raw)));
        } catch (NumberFormatException e) {
            throw new InvalidPersonException("Choose a patron");
        }
    }
}
