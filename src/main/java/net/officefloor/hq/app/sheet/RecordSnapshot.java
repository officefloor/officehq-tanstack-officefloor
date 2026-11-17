package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;

/** Before a new today takes effect, writes SNAPSHOT for every person, in list order, with their due-total as it stood
 *  on the day being left — ahead of any period it closes and the figures it moves. A today equal to the stored one
 *  changes nothing and writes nothing. While the score log is on, SCORE for every person, in list order, with their
 *  score on the day being left, comes first. Each is also stored (see SnapshotRepository). */
public class RecordSnapshot {
    public void record(@Val Sheet sheet, @Val LocalDate today, @Val TodayBy by, @Val RollupBefore before, Audit audit,
            SnapshotRepository snapshots, SettingRepository settings) {
        if (today.equals(sheet.today())) {
            return;
        }
        if (settings.scoreLog(sheet.id())) {
            for (Figures f : before.figures().values()) {
                audit.record("SCORE sheet=" + sheet.id() + " by=" + by.by() + " name=" + f.name() + " score=" + f.score());
            }
        }
        for (java.util.Map.Entry<Long, Figures> e : before.figures().entrySet()) {
            Figures f = e.getValue();
            snapshots.add(sheet.id(), e.getKey(), sheet.today(), today, f.dueTotal());
            audit.record("SNAPSHOT sheet=" + sheet.id() + " by=" + by.by() + " name=" + f.name() + " due-total="
                    + f.dueTotal());
        }
    }
}
