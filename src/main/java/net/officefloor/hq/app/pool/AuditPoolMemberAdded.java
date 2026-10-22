package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored pool member in the audit file. */
public class AuditPoolMemberAdded {
    public void audit(@Val PoolMember m, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + m.sheetId() + " by=" + m.by() + " pool=" + m.pool() + " name="
                + m.name() + " share=" + m.share().toPlainString());
    }
}
