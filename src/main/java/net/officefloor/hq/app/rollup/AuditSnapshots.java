package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.TodayRequest;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Before the sheet's today changes: a SNAPSHOT line per person, in list order, with their due-total on the day being left. */
public class AuditSnapshots {
    public void audit(@PathVariable("sheetId") String sheetId, @Val TodayRequest request, @Val Rollup before,
            Audit audit) {
        String id = sheetId.trim();
        for (Rollup.Figures f : before.people().values()) {
            audit.record("SNAPSHOT sheet=" + id + " by=" + request.user() + " name=" + f.name()
                    + " due-total=" + f.dueTotal());
        }
    }
}
