package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import java.util.Map;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/**
 * A change of the sheet's today first records one SNAPSHOT line per person, in list order, with their due-total as it
 * stood on the day being left, each also kept as one of the person's snapshot lines. Runs before the new today is
 * stored, ahead of any period closing and the rollup lines.
 */
public class RecordSnapshots {
    public void record(@Val SheetResponse sheet, @Val LocalDate today, @Val RollupBefore before, @Val ChangedBy by,
            SheetRepository sheets, PersonSnapshotRepository snapshots, Audit audit) {
        LocalDate leaving = sheets.today(sheet.id());
        if (today.equals(leaving)) {
            return;
        }
        for (Map.Entry<Long, Figures> e : before.snapshot().people().entrySet()) {
            Figures f = e.getValue();
            snapshots.insert(sheet.id(), e.getKey(), leaving, f.dueTotal());
            audit.record("SNAPSHOT sheet=" + sheet.id() + " by=" + by.name() + " name=" + f.name() + " due-total="
                    + (f.dueTotal() == null ? "n/a" : f.dueTotal().toPlainString()));
        }
    }
}
