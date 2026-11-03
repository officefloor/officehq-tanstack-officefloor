package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the member form: a person must be chosen. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberRequest> member)
            throws BadRequestException {
        if (request == null || request.personId() == null) {
            throw new BadRequestException("choose a person");
        }
        member.set(new CrewMemberRequest(request.crew() == null ? "" : request.crew(), request.personId()));
    }
}
