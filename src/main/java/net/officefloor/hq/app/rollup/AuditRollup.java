package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Follows a change's own records with one ROLLUP line per person whose figures it moved (a person new to the
 * sheet counts as moved), in the order they were added, then a TOTALS line when the sheet's totals moved.
 */
public class AuditRollup {
    public void audit(@Val RollupBefore before, @PathVariable("sheetId") String sheetId,
            @RequestParam("user") String user, RollupService rollups, Audit audit) {
        RollupLines.record(before.rollup(), rollups.compute(sheetId), sheetId, user, audit);
    }
}
