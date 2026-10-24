package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the member's person from the body; refuses a missing one. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberRequest> body)
            throws InvalidCrewException {
        if (request == null || request.personId() == null) {
            throw new InvalidCrewException("Choose a person for the crew.");
        }
        body.set(request);
    }
}
