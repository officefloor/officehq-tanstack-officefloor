package net.officefloor.hq.app.person;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Whether to hold or release the person. Publishes it, with the user making the change. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldInput> hold, Out<ChangedBy> changedBy)
            throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        if (request == null || request.held() == null) {
            throw new BadRequestException("Say whether to hold or release the person");
        }
        hold.set(new HoldInput(request.held()));
        changedBy.set(by);
    }
}
