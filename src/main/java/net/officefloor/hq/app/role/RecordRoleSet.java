package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes ROLE_SET to the audit file once the role is stored. */
public class RecordRoleSet {
    public void record(@Val Role role, @Val RoleValues values, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + values.by() + " user=" + role.user()
                + " role=" + role.role());
    }
}
