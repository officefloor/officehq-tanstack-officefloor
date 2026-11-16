package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes POOL_MEMBER_ADDED to the audit file once the member is stored; from= and to= at the end when given. */
public class RecordPoolMemberAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val PoolMemberValues values, Audit audit) {
        StringBuilder line = new StringBuilder("POOL_MEMBER_ADDED sheet=" + sheetId.trim() + " by=" + values.by()
                + " pool=" + values.pool() + " name=" + values.name() + " share=" + values.share().toPlainString());
        if (values.from() != null) {
            line.append(" from=").append(values.from());
        }
        if (values.to() != null) {
            line.append(" to=").append(values.to());
        }
        audit.record(line.toString());
    }
}
