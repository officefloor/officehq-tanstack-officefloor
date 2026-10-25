package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Publishes the user re-settling the claim. */
public class ValidateResettle {
    public void validate(@RequestBody ResettleRequest request, Out<ChangedBy> changedBy) throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
    }
}
