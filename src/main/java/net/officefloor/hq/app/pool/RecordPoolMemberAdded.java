package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPoolMemberAdded {
    public void record(@Val PoolMemberAdded added, @Val ChangedBy by, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + added.sheetId() + " by=" + by.name() + " pool=" + added.pool()
                + " name=" + added.name() + " share=" + added.share().toPlainString());
    }
}
