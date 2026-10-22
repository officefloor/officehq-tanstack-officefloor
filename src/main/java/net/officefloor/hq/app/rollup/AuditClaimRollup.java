package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Record, after the change's own lines, how the sheet's figures moved over it. */
public class AuditClaimRollup {
    public void audit(@PathVariable("sheetId") String sheetId, @Val ClaimInput request, @Val Rollup before,
            Rollups rollups) {
        rollups.record(sheetId.trim(), request.user(), before);
    }
}
