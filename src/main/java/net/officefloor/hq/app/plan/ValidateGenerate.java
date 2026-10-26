package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGenerate {
    public void validate(@RequestBody GenerateRequest request, Out<ChangedBy> changedBy) throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
    }
}
