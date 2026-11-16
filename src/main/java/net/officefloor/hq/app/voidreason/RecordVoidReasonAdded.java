package net.officefloor.hq.app.voidreason;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes VOID_REASON_ADDED to the audit file once the void reason is stored. */
public class RecordVoidReasonAdded {
    public void record(@PathVariable("sheetId") String sheetId, @Val VoidReasonValues values, Audit audit) {
        audit.record("VOID_REASON_ADDED sheet=" + sheetId.trim() + " by=" + values.by() + " reason=" + values.reason());
    }
}
