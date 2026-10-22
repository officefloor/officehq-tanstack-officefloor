package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored role in the audit file. */
public class AuditRoleSet {
    public void audit(@Val RoleInput input, @Val Role role, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + input.user()
                + " user=" + role.user() + " role=" + role.role());
    }
}
