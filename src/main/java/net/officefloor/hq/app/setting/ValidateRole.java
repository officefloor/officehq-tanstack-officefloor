package net.officefloor.hq.app.setting;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRole {

    private static final Set<String> ROLES = Set.of(Settings.CLERK, Settings.STEWARD, Settings.VIEWER);

    // A user name must not be blank, and the role must be clerk, steward or viewer.
    public void validate(@PathVariable("sheetId") String sheetId, @PathVariable("user") String user,
            @RequestBody RoleRequest body, Out<Role> roleOut) throws InvalidSettingException {
        String name = user == null ? "" : user.trim();
        String role = body == null || body.role() == null ? "" : body.role().trim();
        if (name.isEmpty()) {
            throw new InvalidSettingException("A user name is needed.");
        }
        if (!ROLES.contains(role)) {
            throw new InvalidSettingException("The role must be clerk, steward or viewer.");
        }
        roleOut.set(new Role(sheetId.trim(), name, role));
    }
}
