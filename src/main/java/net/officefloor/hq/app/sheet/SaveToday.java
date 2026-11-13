package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimStore;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Stores the sheet's today; a new today releases every claim's lock (each open editor's). Before a new today
 *  takes effect, each person's due-total as it stood on the day being left is recorded, in list order, as
 *  SNAPSHOT sheet=<id> by=<user> name=<name> due-total=<due-total>, and kept on the person with that day. While the
 *  sheet's score log is on, each person's score on the day being left is recorded first, in list order, as
 *  SCORE sheet=<id> by=<user> name=<name> score=<score>. */
public class SaveToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today, @Val TodayRequest request,
            @Val Rollup before, SheetStore store, ClaimStore claims, PersonStore people, SettingStore settings,
            Audit audit) {
        String id = sheetId.trim();
        LocalDate leaving = store.today(id);
        // The new today is stored first, so the snapshots are kept as made on it (a sheet read as of the day
        // being left does not show them yet).
        store.setToday(id, today);
        if (!today.equals(leaving)) {
            if (settings.find(id, SettingStore.SCORE_LOG) != null) {
                before.people().values().forEach(figures -> audit.record("SCORE sheet=" + id + " by=" + request.user()
                        + " name=" + figures.name() + " score=" + figures.score()));
            }
            before.people().forEach((personId, figures) -> {
                people.snapshot(id, personId, leaving, figures.dueTotal());
                audit.record("SNAPSHOT sheet=" + id + " by=" + request.user() + " name=" + figures.name()
                        + " due-total=" + figures.dueTotal());
            });
            claims.unlockAll(id);
        }
    }
}
