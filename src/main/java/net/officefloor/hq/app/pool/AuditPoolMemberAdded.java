package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the member added to a pool. */
public class AuditPoolMemberAdded {
    public void audit(@Val NewPoolMember added, @RequestParam("user") String user, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + added.pool().sheetId() + " by=" + user + " pool="
                + added.pool().name() + " name=" + added.member().name() + " share="
                + added.member().share().toPlainString()
                + (added.member().from() == null ? "" : " from=" + added.member().from())
                + (added.member().to() == null ? "" : " to=" + added.member().to()));
    }
}
