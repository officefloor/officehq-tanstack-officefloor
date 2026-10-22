package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.RedateInput;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Record, after the change's own lines, how the sheet's figures moved over it. */
public class AuditRedateRollup {
    public void audit(@PathVariable("sheetId") String sheetId, @Val RedateInput request, @Val Rollup before,
            Rollups rollups) {
        rollups.record(sheetId.trim(), request.user(), before);
    }
}
