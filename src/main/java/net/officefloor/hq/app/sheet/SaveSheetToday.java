package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.period.PeriodCloser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Set the sheet's today (a new date first records, while the score log is on, each person's score, then each person's due-total, as they stood on the day left, and releases every claim's editor lock), then close each open period lapsed past the sheet's grace days. */
public class SaveSheetToday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate today,
            ServerHttpConnection connection, SheetRepository repository, Rollup rollup, PeriodCloser closer, ClaimRepository claims,
            SettingsRepository settings,
            Out<Sheet> sheet)
            throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        Rollup.Snapshot before = rollup.take(id);
        // A change of today under a delegation is marked for its steward as the delegation stood on the day left.
        String delegator = settings.delegator(id, by);
        LocalDate was = repository.today(id);
        if (!repository.setToday(id, today)) {
            throw new BadRequestException("no such sheet");
        }
        if (!today.equals(was)) {
            if (settings.scoreLog(id)) rollup.scores(id, by, before);
            rollup.snapshot(id, by, before, delegator, was);
            claims.unlockAll(id);
        }
        closer.closeLapsed(id, by, today);
        rollup.record(id, by, before);
        sheet.set(new Sheet(id, today.toString(), repository.archived(id)));
    }
}
