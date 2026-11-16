package net.officefloor.hq.app.role;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the user name is not blank, the role clerk or steward. */
public class ValidateRole {
    private static final Set<String> ROLES = Set.of(RoleRepository.CLERK, RoleRepository.STEWARD);

    public void validate(@PathVariable("user") String user, @RequestBody RoleRequest request, Out<RoleValues> values)
            throws InvalidRoleException {
        String u = user == null ? "" : user.trim();
        if (u.isEmpty()) {
            throw new InvalidRoleException("The user name must not be blank");
        }
        String role = request == null || request.role() == null ? "" : request.role().trim();
        if (!ROLES.contains(role)) {
            throw new InvalidRoleException("The role must be clerk or steward");
        }
        values.set(new RoleValues(u, role, request.by() == null ? "" : request.by().trim()));
    }
}
