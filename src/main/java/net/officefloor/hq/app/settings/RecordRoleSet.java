package net.officefloor.hq.app.settings;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the role is saved, so a refused change writes no audit line. */
public class RecordRoleSet {
    public void record(@Val RoleChange change, Audit audit) {
        audit.record("ROLE_SET sheet=" + change.sheetId() + " by=" + change.by()
                + " user=" + change.name() + " role=" + change.role().show());
    }
}
