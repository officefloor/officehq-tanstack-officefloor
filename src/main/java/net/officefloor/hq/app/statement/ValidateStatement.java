package net.officefloor.hq.app.statement;

import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a statement by a user who may not change the sheet, or as of something that is not a date. */
public class ValidateStatement {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody StatementRequest request, Out<StatementRequest> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseChange(sheetId.trim(), request == null ? null : request.user());
        try {
            if (request != null) {
                request.asOfDate();
            }
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("The statement date is not a date");
        }
        body.set(request == null ? new StatementRequest(null, null) : request);
    }
}
