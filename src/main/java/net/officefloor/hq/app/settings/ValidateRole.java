package net.officefloor.hq.app.settings;

import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the role form: a user name (no spaces) and a role, clerk, steward or viewer. */
public class ValidateRole {
    private static final Set<String> ROLES = Set.of(SettingsRepository.CLERK, SettingsRepository.STEWARD, SettingsRepository.VIEWER);

    public void validate(@RequestBody RoleRequest request, Out<Role> role) throws BadRequestException {
        String user = request == null || request.user() == null ? "" : request.user().trim();
        String name = request == null || request.role() == null ? "" : request.role().trim();
        if (user.isEmpty()) {
            throw new BadRequestException("user is required");
        }
        if (user.chars().anyMatch(Character::isWhitespace)) {
            throw new BadRequestException("user must not contain spaces");
        }
        if (!ROLES.contains(name)) {
            throw new BadRequestException("role must be clerk, steward or viewer");
        }
        role.set(new Role(user, name));
    }
}
