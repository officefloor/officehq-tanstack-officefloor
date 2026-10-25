package net.officefloor.hq.app.access;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordRoleSet {
    public void record(@Val Role role, @Val ChangedBy by, Audit audit) {
        audit.record("ROLE_SET sheet=" + role.sheetId() + " by=" + by.name() + " user=" + role.user() + " role="
                + role.role());
    }
}
