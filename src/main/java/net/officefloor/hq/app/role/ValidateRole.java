package net.officefloor.hq.app.role;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user, a user name, and a role
 *  of clerk or steward. */
public class ValidateRole {
    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD);

    public void validate(@RequestBody RoleRequest request, Out<RoleInput> body) throws InvalidRoleException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidRoleException("Sign in before changing roles.");
        }
        String name = request.name() == null ? "" : request.name().trim();
        if (name.isEmpty() || name.length() > 100) {
            throw new InvalidRoleException("Give the user name the role is for.");
        }
        String role = request.role() == null ? "" : request.role().trim();
        if (!ROLES.contains(role)) {
            throw new InvalidRoleException("The role must be clerk or steward.");
        }
        body.set(new RoleInput(request.user().trim(), name, role));
    }
}
