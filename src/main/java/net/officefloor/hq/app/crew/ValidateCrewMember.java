package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a crew and a person are named. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberRequest> body)
            throws InvalidCrewException {
        if (request == null || request.crew() == null || request.crew().trim().isEmpty() || request.personId() == null) {
            throw new InvalidCrewException("A crew member needs a crew and a person");
        }
        body.set(request);
    }
}
