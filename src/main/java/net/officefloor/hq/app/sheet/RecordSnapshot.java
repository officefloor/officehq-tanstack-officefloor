package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Before a new today takes effect, writes SNAPSHOT for every person, in list order, with their due-total as it stood
 *  on the day being left — ahead of any period it closes and the figures it moves. A today equal to the stored one
 *  changes nothing and writes nothing. Each is also stored (see SnapshotRepository). */
public class RecordSnapshot {
    public void record(@Val Sheet sheet, @Val LocalDate today, @Val TodayBy by, @Val RollupBefore before, Audit audit,
            SnapshotRepository snapshots) {
        if (today.equals(sheet.today())) {
            return;
        }
        for (java.util.Map.Entry<Long, Figures> e : before.figures().entrySet()) {
            Figures f = e.getValue();
            snapshots.add(sheet.id(), e.getKey(), sheet.today(), today, f.dueTotal());
            audit.record("SNAPSHOT sheet=" + sheet.id() + " by=" + by.by() + " name=" + f.name() + " due-total="
                    + f.dueTotal());
        }
    }
}
