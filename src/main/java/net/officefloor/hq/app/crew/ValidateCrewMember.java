package net.officefloor.hq.app.crew;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a person. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberInput> body)
            throws InvalidCrewException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidCrewException("Sign in before adding a crew member.");
        }
        if (request.personId() == null) {
            throw new InvalidCrewException("Choose the person to add.");
        }
        body.set(new CrewMemberInput(request.user().trim(), request.personId()));
    }
}
