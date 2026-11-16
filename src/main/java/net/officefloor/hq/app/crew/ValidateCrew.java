package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the crew's name, trimmed, is not blank, and a lead is chosen. */
public class ValidateCrew {
    public void validate(@RequestBody CrewRequest request, Out<CrewRequest> body) throws InvalidCrewException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("A crew's name must not be blank");
        }
        if (request.leadId() == null) {
            throw new InvalidCrewException("A crew needs a lead");
        }
        body.set(request);
    }
}
