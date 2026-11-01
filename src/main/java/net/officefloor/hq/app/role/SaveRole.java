package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveRole {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Role role,
            Roles roles, Audit audit) {
        roles.set(sheet, role);
        audit.record("ROLE_SET sheet=" + sheet + " by=" + user + " user=" + role.user() + " role=" + role.role());
    }
}
