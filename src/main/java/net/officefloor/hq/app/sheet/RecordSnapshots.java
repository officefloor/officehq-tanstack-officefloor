package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Before a new today takes effect: one SNAPSHOT line per person, in list order, with the
 *  due-total as it stood on the day being left, kept on the person too; nothing when the today does not change. */
public class RecordSnapshots {
    public void record(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val LocalDate today,
            @Val Rollup before, SheetRepository sheets, PersonRepository people,
            Audit audit) {
        String id = sheetId.trim();
        LocalDate leaving = sheets.today(id);
        if (today.equals(leaving)) return;
        String head = "SNAPSHOT sheet=" + id + " by=" + (by == null ? "" : by);
        before.people().forEach((personId, figures) -> {
            String due = figures.dueTotal() == null ? "n/a" : figures.dueTotal();
            people.addSnapshot(id, personId, leaving, due);
            audit.record(head + " name=" + figures.name() + " due-total=" + due);
        });
    }
}
