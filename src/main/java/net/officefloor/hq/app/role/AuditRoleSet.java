package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the role once it is valid and stored. */
public class AuditRoleSet {
    public void audit(@Val Role role, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + user
                + " user=" + role.user() + " role=" + role.role());
    }
}
