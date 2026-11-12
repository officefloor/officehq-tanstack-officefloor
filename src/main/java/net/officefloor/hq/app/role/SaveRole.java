package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRole {
    public void save(@PathVariable("sheetId") String sheetId, @Val RoleInput role, RoleStore store) {
        store.set(sheetId.trim(), role.user(), role.role());
    }
}
