package net.officefloor.hq.app.person;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a merge undo by anyone but a steward. */
public class ValidateMergeUndo {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody MergeUndoRequest request,
            RoleStore roles, Out<MergeUndoRequest> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        roles.refuseChange(sheetId.trim(), user);
        if (roles.find(sheetId.trim(), user) != Role.STEWARD) {
            throw new InvalidSheetException("Only a steward may undo a merge");
        }
        body.set(new MergeUndoRequest(user));
    }
}
