package net.officefloor.hq.app.role;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    public void validate(@RequestBody RoleRequest request, Out<RoleInput> body) throws InvalidSheetException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSheetException("A user name is required");
        }
        String text = request.role() == null ? "" : request.role().trim();
        Role role = Role.of(text);
        if (role == null) {
            throw new InvalidSheetException("The role must be clerk or steward");
        }
        body.set(new RoleInput(user, role, request.by()));
    }
}
