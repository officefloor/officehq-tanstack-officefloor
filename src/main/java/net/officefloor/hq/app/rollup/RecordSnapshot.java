package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.sheet.LeftDay;
import net.officefloor.plugin.variable.Val;

/** Runs as a new today takes effect: when the today moves, one SNAPSHOT line per person, in list
 *  order, with their due-total as it stood on the day being left (n/a when it is n/a), each also kept
 *  on the person as a snapshot line. Kept once the new today is set, so the sheet as it stood before
 *  the move (see {@link net.officefloor.hq.app.sheet.AsOf}) does not have it. */
public class RecordSnapshot {
    public void record(@Val RollupBefore before, @Val LocalDate today, @Val LeftDay left, @Val Actor actor, People people,
            Audit audit) {
        LocalDate leaving = left.day();
        if (today.equals(leaving)) return;
        String prefix = " sheet=" + before.sheetId() + " by=" + actor.user();
        before.snapshot().people().forEach((id, was) -> {
            audit.record("SNAPSHOT" + prefix + " name=" + was.name()
                    + " due-total=" + (was.dueTotal() == null ? "n/a" : was.dueTotal().setScale(2).toPlainString()));
            people.addSnapshot(before.sheetId(), id, leaving, was.dueTotal());
        });
    }
}
