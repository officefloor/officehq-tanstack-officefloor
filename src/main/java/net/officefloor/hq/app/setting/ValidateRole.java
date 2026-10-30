package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;

public class ValidateRole {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RoleRequest request, Out<ValidRole> body, Settings settings)
            throws InvalidSettingException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSettingException("A role needs a user name");
        }
        String role = request.role() == null ? "" : request.role().trim();
        if (!Settings.CLERK.equals(role) && !Settings.STEWARD.equals(role) && !Settings.VIEWER.equals(role)) {
            throw new InvalidSettingException("The role must be clerk, steward or viewer");
        }
        body.set(new ValidRole(name, role, request.user()));
    }
}
