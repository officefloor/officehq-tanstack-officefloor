package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Publishes the user taking the statement. */
public class ValidateStatement {
    public void validate(@RequestBody StatementRequest request, Out<ChangedBy> changedBy) throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
    }
}
