package net.officefloor.hq.app.rollup;

import java.util.Map;
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
        Rollup was = before.rollup();
        Rollup now = rollups.compute(sheetId);
        String prefix = " sheet=" + sheetId + " by=" + user;
        for (Map.Entry<Long, Rollup.Figures> entry : now.people().entrySet()) {
            Rollup.Figures figures = entry.getValue();
            if (!figures.sameAs(was.people().get(entry.getKey()))) {
                audit.record("ROLLUP" + prefix + " person=" + figures.name() + " claimed=" + figures.claimed()
                        + " due-total=" + figures.dueTotal() + " overdue-total=" + figures.overdueTotal());
            }
        }
        if (!now.claimedTotal().equals(was.claimedTotal()) || !now.dueTotal().equals(was.dueTotal())
                || !now.overdueTotal().equals(was.overdueTotal())) {
            audit.record("TOTALS" + prefix + " claimed-total=" + now.claimedTotal() + " due-total="
                    + now.dueTotal() + " overdue-total=" + now.overdueTotal());
        }
    }
}
