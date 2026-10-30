package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    public void validate(@RequestBody RoleRequest request, Out<ValidRole> body) throws InvalidSettingException {
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidSettingException("A role needs a user name");
        }
        String role = request.role() == null ? "" : request.role().trim();
        if (!Settings.CLERK.equals(role) && !Settings.STEWARD.equals(role)) {
            throw new InvalidSettingException("The role must be clerk or steward");
        }
        body.set(new ValidRole(name, role, request.user()));
    }
}
