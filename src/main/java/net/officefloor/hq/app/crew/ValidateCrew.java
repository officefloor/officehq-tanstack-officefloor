package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the crew's name, trimmed, and lead from the body; refuses a blank name or a missing lead. */
public class ValidateCrew {
    public void validate(@RequestBody CrewRequest request, Out<CrewRequest> body) throws InvalidCrewException {
        String value = request == null || request.name() == null ? "" : request.name().trim();
        if (value.isEmpty()) {
            throw new InvalidCrewException("A crew's name cannot be blank.");
        }
        if (request.leadId() == null) {
            throw new InvalidCrewException("Choose a lead for the crew.");
        }
        body.set(new CrewRequest(value, request.leadId()));
    }
}
