package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored role: ROLE_SET sheet=<id> by=<user> user=<name> role=<role>. */
public class AuditRoleSet {
    public void audit(@PathVariable("sheetId") String sheetId, @Val RoleInput role, Audit audit) {
        audit.record("ROLE_SET sheet=" + sheetId.trim() + " by=" + role.by() + " user=" + role.user() + " role="
                + role.role().shown());
    }
}
