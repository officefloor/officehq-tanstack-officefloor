package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Before a new today takes effect: one SNAPSHOT line per person, in list order, with the
 *  due-total as it stood on the day being left; nothing when the today does not change. */
public class RecordSnapshots {
    public void record(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "by", required = false) String by, @Val LocalDate today,
            @Val Rollup before, SheetRepository sheets, Audit audit) {
        String id = sheetId.trim();
        if (today.equals(sheets.today(id))) return;
        String head = "SNAPSHOT sheet=" + id + " by=" + (by == null ? "" : by);
        before.people().values().forEach(figures -> audit.record(head + " name=" + figures.name()
                + " due-total=" + (figures.dueTotal() == null ? "n/a" : figures.dueTotal())));
    }
}
