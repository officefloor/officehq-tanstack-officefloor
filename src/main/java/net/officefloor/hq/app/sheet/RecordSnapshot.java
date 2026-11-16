package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Figures;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Before a new today takes effect, writes SNAPSHOT for every person, in list order, with their due-total as it stood
 *  on the day being left — ahead of any period it closes and the figures it moves. A today equal to the stored one
 *  changes nothing and writes nothing. */
public class RecordSnapshot {
    public void record(@Val Sheet sheet, @Val LocalDate today, @Val TodayBy by, @Val RollupBefore before, Audit audit) {
        if (today.equals(sheet.today())) {
            return;
        }
        for (Figures f : before.figures().values()) {
            audit.record("SNAPSHOT sheet=" + sheet.id() + " by=" + by.by() + " name=" + f.name() + " due-total="
                    + f.dueTotal());
        }
    }
}
