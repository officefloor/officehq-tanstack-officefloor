package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {
    public void validate(@RequestBody RoleRequest request, Out<RoleInput> body) throws InvalidRoleException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        if (user.isEmpty()) throw new InvalidRoleException("A user name is required");
        String role = request.role() == null ? "" : request.role().trim();
        if (!Role.CLERK.equals(role) && !Role.STEWARD.equals(role)) {
            throw new InvalidRoleException("A role is clerk or steward");
        }
        body.set(new RoleInput(user, role));
    }
}
