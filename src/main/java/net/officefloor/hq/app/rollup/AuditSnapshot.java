package net.officefloor.hq.app.rollup;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.history.HistoryRecorder;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonSnapshot;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** When the sheet's today changes, records each person's due-total as it stood on the day being left, before the
 *  change takes effect (so before any period closes and before the change's rollup lines), and keeps it on the
 *  person as a snapshot of that day (recorded in its history on that day, so the sheet as of it shows it). */
public class AuditSnapshot {
    public void audit(@Val LocalDate today, @Val Sheet sheet, @Val Rollup before,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit, PersonRepository people,
            HistoryRecorder recorder) {
        if (today.equals(sheet.getToday())) {
            return;
        }
        before.dueTotals().forEach((id, due) -> people.findById(id).ifPresent(p -> {
            p.addSnapshot(new PersonSnapshot(sheet.getToday(), due));
            recorder.recordOn(p, sheet.getToday());
            people.save(p);
        }));
        before.snapshot(sheet.getId(), AuditUser.decode(user)).forEach(audit::record);
    }
}
