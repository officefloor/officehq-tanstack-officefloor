package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses the cancelling of a statement by anyone but a steward. */
public class ValidateCancelStatement {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody StatementRequest request,
            Out<StatementRequest> body, RoleStore roles) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward can cancel a statement");
        }
        body.set(request);
    }
}
