package net.officefloor.hq.app.role;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the role form from the body; refuses a blank user name and a role other than clerk, steward, viewer or restricted. */
public class ValidateRole {

    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD, Role.VIEWER, Role.RESTRICTED);

    public void validate(@RequestBody RoleRequest request, Out<RoleInput> body) throws InvalidRoleException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String role = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new InvalidRoleException("The user name must not be blank.");
        }
        if (!ROLES.contains(role)) {
            throw new InvalidRoleException("The role must be clerk, steward, viewer or restricted.");
        }
        body.set(new RoleInput(user, role));
    }
}
