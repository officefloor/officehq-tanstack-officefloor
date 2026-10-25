package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The person a claim is to move to. Publishes it with the user making the change. */
public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignTo> to, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.personId() == null) {
            throw new BadRequestException("Choose the person to move the claim to");
        }
        to.set(new ReassignTo(request.personId()));
        changedBy.set(by);
    }
}
