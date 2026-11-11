package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A member needs a crew and a person. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberInput> body) throws InvalidCrewException {
        if (request == null) throw new InvalidCrewException("A crew member is required");
        String crew = request.crew() == null ? "" : request.crew().trim();
        if (crew.isEmpty()) throw new InvalidCrewException("A crew is required");
        body.set(new CrewMemberInput(crew, ValidateCrew.id(request.personId(), "A person is required")));
    }
}
