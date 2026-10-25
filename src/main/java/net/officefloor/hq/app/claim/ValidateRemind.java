package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The user reminding the claim. */
public class ValidateRemind {
    public void validate(@RequestBody RemindRequest request, Out<ChangedBy> changedBy) throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
    }
}
