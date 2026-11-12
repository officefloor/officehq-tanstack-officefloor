package net.officefloor.hq.app.pool;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a pool member added: POOL_MEMBER_ADDED sheet=<id> by=<user> pool=<pool> name=<person> share=<share>
 *  [from=<date>] [to=<date>], each date only when given. */
public class AuditMemberAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val MemberInput input, Audit audit) {
        audit.record("POOL_MEMBER_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " pool="
                + input.pool().name() + " name=" + input.personName() + " share=" + input.share().toPlainString()
                + (input.from() == null ? "" : " from=" + input.from())
                + (input.to() == null ? "" : " to=" + input.to()));
    }
}
