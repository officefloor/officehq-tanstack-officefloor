package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the member is saved, so a refused one writes no audit line. */
public class RecordPoolMemberAdded {
    public void record(@Val PoolMember member, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + member.sheetId() + " by=" + member.by() + " pool=" + member.pool()
                + " name=" + member.name() + " share=" + member.share().toPlainString()
                + (member.from() == null ? "" : " from=" + member.from())
                + (member.to() == null ? "" : " to=" + member.to()));
    }
}
