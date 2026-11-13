package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Before the sheet's today changes, records one SNAPSHOT line per person, in list order, with their due-total as it
 * stood on the day being left.
 */
public class AuditSnapshot {
    public void audit(@Val RollupBefore before, @PathVariable("sheetId") String sheetId,
            @RequestParam("user") String user, Audit audit) {
        for (Rollup.Figures figures : before.rollup().people().values()) {
            audit.record("SNAPSHOT sheet=" + sheetId + " by=" + user + " name=" + figures.name()
                    + " due-total=" + figures.dueTotal());
        }
    }
}
