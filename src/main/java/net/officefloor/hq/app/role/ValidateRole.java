package net.officefloor.hq.app.role;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD);

    public void validate(@RequestBody RoleRequest request, Out<Role> body) throws InvalidRoleException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String role = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new InvalidRoleException("A role needs a user name");
        }
        if (!ROLES.contains(role)) {
            throw new InvalidRoleException("The role must be clerk or steward");
        }
        body.set(new Role(user, role));
    }
}
