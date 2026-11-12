package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a void reason added: VOID_REASON_ADDED sheet=<id> by=<user> reason=<reason>. */
public class AuditVoidReasonAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val VoidReasonInput input, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " reason=" + input.reason());
    }
}
