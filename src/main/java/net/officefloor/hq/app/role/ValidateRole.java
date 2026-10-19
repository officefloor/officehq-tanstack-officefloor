package net.officefloor.hq.app.role;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {

    private static final Set<String> ROLES = Set.of(Role.CLERK, Role.STEWARD);

    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("userName") String userName,
            @RequestBody RoleRequest request, Out<Role> role) throws InvalidRoleException {
        String user = userName == null ? "" : userName.strip();
        String value = request == null || request.role() == null ? "" : request.role().strip();
        if (user.isEmpty()) {
            throw new InvalidRoleException("A user name is required.");
        }
        if (!ROLES.contains(value)) {
            throw new InvalidRoleException("The role must be clerk or steward.");
        }
        role.set(new Role(sheetId, user, value));
    }
}
