package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a name that is not blank and a
 *  lead. */
public class ValidateCrew {
    public void validate(@RequestBody CrewRequest request, Out<CrewInput> body) throws InvalidCrewException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidCrewException("Sign in before adding a crew.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("Give the crew a name.");
        }
        if (request.leadId() == null) {
            throw new InvalidCrewException("Choose the crew's lead.");
        }
        body.set(new CrewInput(request.user().trim(), name, request.leadId()));
    }
}
