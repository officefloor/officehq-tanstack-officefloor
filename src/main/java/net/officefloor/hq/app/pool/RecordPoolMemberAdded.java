package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes POOL_MEMBER_ADDED to the audit file once the member is stored. */
public class RecordPoolMemberAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val PoolMemberValues values, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " pool=" + values.pool()
                + " name=" + values.name() + " share=" + values.share().toPlainString());
    }
}
