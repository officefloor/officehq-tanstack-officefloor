package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a void reason by a viewer, a blank one, or one already on the sheet's list. */
public class ValidateVoidReason {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody VoidReasonRequest request,
            Out<VoidReasonInput> body, RoleStore roles, VoidReasonStore reasons) throws InvalidSheetException {
        String sheet = sheetId.trim();
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheet, user);
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidSheetException("A reason is required");
        }
        if (reasons.has(sheet, reason)) {
            throw new InvalidSheetException(reason + " is already a void reason");
        }
        body.set(new VoidReasonInput(reason, user));
    }
}
