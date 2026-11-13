package net.officefloor.hq.app.setting;

import jakarta.validation.Valid;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRoleRequest {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody @Valid RoleRequest request,
            Out<UserRole> role) throws InvalidSettingException {
        String user = request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) {
            throw new InvalidSettingException("A user name is required.");
        }
        Role found = Role.of(request.role());
        if (found == null) {
            throw new InvalidSettingException("The role must be clerk, steward or viewer.");
        }
        role.set(new UserRole(sheetId, user, found));
    }
}
