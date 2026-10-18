package net.officefloor.hq.app.setting;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the role from the body, refusing an empty user name or a role other than clerk or steward. */
public class ValidateRole {

    private static final Set<String> ROLES = Set.of(UserRole.CLERK, UserRole.STEWARD);

    public void validate(@RequestBody RoleRequest request, Out<RoleInput> roleOut) throws InvalidSettingException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String role = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new InvalidSettingException("The user name must not be empty.");
        }
        if (!ROLES.contains(role)) {
            throw new InvalidSettingException("The role must be clerk or steward.");
        }
        roleOut.set(new RoleInput(user, role));
    }
}
