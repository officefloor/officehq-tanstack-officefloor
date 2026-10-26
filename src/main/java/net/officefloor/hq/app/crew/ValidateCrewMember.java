package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A member to add: a crew and a person. */
public class ValidateCrewMember {
    public void validate(@RequestBody CrewMemberRequest request, Out<CrewMemberInput> member, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.crew() == null || request.crew().isBlank()) {
            throw new BadRequestException("Choose a crew");
        }
        if (request.person() == null) {
            throw new BadRequestException("Choose a person");
        }
        member.set(new CrewMemberInput(request.crew().trim(), request.person()));
        changedBy.set(by);
    }
}
