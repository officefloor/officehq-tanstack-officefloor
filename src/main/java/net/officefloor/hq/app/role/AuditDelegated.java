package net.officefloor.hq.app.role;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a delegation: DELEGATED sheet=<id> by=<steward> user=<user> from=<date> to=<date>. */
public class AuditDelegated {
    public void audit(@PathVariable("sheetId") String sheetId, @Val Delegation d, Audit audit) {
        audit.record("DELEGATED sheet=" + sheetId.trim() + " by=" + d.by() + " user=" + d.user() + " from="
                + d.from() + " to=" + d.to());
    }
}
