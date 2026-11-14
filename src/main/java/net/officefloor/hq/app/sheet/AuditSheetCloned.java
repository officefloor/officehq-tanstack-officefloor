package net.officefloor.hq.app.sheet;

import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupLines;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Records the clone on this sheet (SHEET_CLONE_MADE) and on the clone (SHEET_CLONED, its first line), followed there
 * by its rollup lines for every person and its totals.
 */
public class AuditSheetCloned {
    public void audit(@Val SheetClone clone, @RequestParam("user") String user, RollupService rollups, Audit audit) {
        audit.record("SHEET_CLONE_MADE sheet=" + clone.from() + " by=" + user + " to=" + clone.to());
        audit.record("SHEET_CLONED sheet=" + clone.to() + " by=" + user + " from=" + clone.from());
        RollupLines.record(new Rollup(Map.of(), "", "", "", null), rollups.compute(clone.to()), clone.to(), user, audit);
    }
}
