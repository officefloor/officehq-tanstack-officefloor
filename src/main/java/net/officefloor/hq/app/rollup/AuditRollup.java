package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** After the change's own audit lines: a ROLLUP line per person whose figures moved, then a TOTALS
 *  line when the sheet's totals moved. A change that moves no figure writes none. */
public class AuditRollup {
    public void audit(@PathVariable("sheetId") String sheetId, @Val Rollup.Snapshot before,
            @RequestParam(value = "user", required = false) String user, Rollup rollup, Audit audit) {
        rollup.changes(sheetId, user, before, rollup.take(sheetId)).forEach(audit::record);
    }
}
