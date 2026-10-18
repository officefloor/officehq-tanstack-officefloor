package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the user name's role on the sheet, replacing any it had. */
public class ApplyRole {
    public void apply(@PathVariable("sheetId") String sheetId, @Val RoleInput input, UserRoleRepository repository,
            Out<UserRole> roleOut) {
        String id = sheetId.trim();
        UserRole role = repository.findBySheetIdAndUser(id, input.user()).orElseGet(() -> new UserRole(id, input.user()));
        role.setRole(input.role());
        roleOut.set(role);
    }
}
