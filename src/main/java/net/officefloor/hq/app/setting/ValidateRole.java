package net.officefloor.hq.app.setting;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD);

    public void validate(@RequestBody RoleRequest request, Out<RoleRequest> body)
            throws InvalidSettingException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String role = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new InvalidSettingException("A user name is required");
        }
        if (!ROLES.contains(role)) {
            throw new InvalidSettingException("Role must be clerk or steward");
        }
        body.set(new RoleRequest(user, role));
    }
}
