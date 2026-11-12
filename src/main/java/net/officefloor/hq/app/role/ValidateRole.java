package net.officefloor.hq.app.role;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RoleRequest request, Out<RoleInput> body, RoleStore roles) throws InvalidSheetException {
        roles.refuseViewer(sheetId.trim(), request == null ? null : request.by());
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSheetException("A user name is required");
        }
        String text = request.role() == null ? "" : request.role().trim();
        Role role = Role.of(text);
        if (role == null) {
            throw new InvalidSheetException("The role must be clerk, steward or viewer");
        }
        body.set(new RoleInput(user, role, request.by()));
    }
}
