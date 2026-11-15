package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a member that went through is recorded. */
public class RecordPoolMemberAdded {
    public void record(@Val PoolMemberAdded member, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + member.by() + " pool="
                + member.pool() + " name=" + member.name() + " share=" + member.share()
                + (member.from() == null ? "" : " from=" + member.from())
                + (member.to() == null ? "" : " to=" + member.to()));
    }
}
