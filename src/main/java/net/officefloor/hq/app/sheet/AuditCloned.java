package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records SHEET_CLONE_MADE sheet=<this> by=<user> to=<clone>, then SHEET_CLONED sheet=<clone> by=<user>
 *  from=<this> followed by the clone's rollup lines for every person and its totals. */
public class AuditCloned {
    public void audit(@PathVariable("sheetId") String sheetId, @Val CloneRequest request, Audit audit,
            RollupService rollups) {
        String from = sheetId.trim();
        audit.record("SHEET_CLONE_MADE sheet=" + from + " by=" + request.user() + " to=" + request.to());
        audit.record("SHEET_CLONED sheet=" + request.to() + " by=" + request.user() + " from=" + from);
        rollups.recordAll(request.to(), request.user());
    }
}
