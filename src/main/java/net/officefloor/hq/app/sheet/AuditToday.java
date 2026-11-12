package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A new today has no audit line of its own; it records only how it moved the sheet's figures. */
public class AuditToday {
    public void audit(@PathVariable("sheetId") String sheetId, @Val TodayRequest request, @Val Rollup before,
            RollupService rollups) {
        rollups.record(sheetId.trim(), request.user(), before);
    }
}
