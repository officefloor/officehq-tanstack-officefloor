package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Before the sheet's today changes: keep, per person, the snapshot written for them — the day being left and their
 *  due-total on it. */
public class SaveSnapshots {
    public void save(@PathVariable("sheetId") String sheetId, @Val Rollup before, SheetRepository sheets,
            SnapshotRepository snapshots) {
        String id = sheetId.trim();
        LocalDate leaving = sheets.today(id);
        for (Map.Entry<Long, Rollup.Figures> e : before.people().entrySet()) {
            snapshots.add(id, e.getKey(), leaving, e.getValue().dueTotal());
        }
    }
}
