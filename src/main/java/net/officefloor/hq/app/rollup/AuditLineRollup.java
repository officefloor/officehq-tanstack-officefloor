package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.LineInput;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Record, after the line's own audit, how the sheet's figures moved over it. */
public class AuditLineRollup {
    public void audit(@PathVariable("sheetId") String sheetId, @Val LineInput request, @Val Rollup before,
            Rollups rollups) {
        rollups.record(sheetId.trim(), request.user(), before);
    }
}
