package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored pool member in the audit file (its from- and to-dates at the end, when given). */
public class AuditPoolMemberAdded {
    public void audit(@Val PoolMember m, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + m.sheetId() + " by=" + m.by() + " pool=" + m.pool() + " name="
                + m.name() + " share=" + m.share().toPlainString() + (m.from() != null ? " from=" + m.from() : "")
                + (m.to() != null ? " to=" + m.to() : ""));
    }
}
