package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses setting an approver by a user who may not change the sheet, or to a name with a space in it. */
public class ValidateApprover {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ApproverRequest request,
            RoleStore roles, Out<ApproverInput> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        String approver = request.approver() == null ? "" : request.approver().trim();
        if (approver.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidSheetException("Not a user name: " + approver);
        }
        body.set(new ApproverInput(approver, user));
    }
}
