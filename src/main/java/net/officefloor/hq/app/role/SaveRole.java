package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Gives the user the role on the sheet, replacing the one they had. */
public class SaveRole {
    public void save(@PathVariable("sheetId") String sheetId, @Val RoleValues values, RoleRepository roles,
            Out<Role> role) {
        role.set(roles.save(new Role(sheetId.trim(), values.user(), values.role())));
    }
}
