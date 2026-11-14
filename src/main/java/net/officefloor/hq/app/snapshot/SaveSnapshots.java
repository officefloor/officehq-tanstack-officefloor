package net.officefloor.hq.app.snapshot;

import java.util.Map;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Before the sheet's today changes, keeps one snapshot line per person: the day being left and their due-total on it. */
public class SaveSnapshots {
    public void save(@Val Sheet sheet, @Val RollupBefore before, SnapshotRepository repository) {
        for (Map.Entry<Long, Rollup.Figures> person : before.rollup().people().entrySet()) {
            repository.save(new Snapshot(sheet.id(), person.getKey(), sheet.today(), person.getValue().dueTotal()));
        }
    }
}
