package net.officefloor.hq.app.role;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveRole {
    public void save(@PathVariable("sheetId") String sheetId, @Val RoleInput input, RoleRepository repository,
            Out<Role> role) {
        role.set(repository.set(sheetId.trim(), input.name(), input.role()));
    }
}
