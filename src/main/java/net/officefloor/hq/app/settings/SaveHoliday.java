package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a date to the sheet's holidays; one already on the list is refused. The due-by of each claim not SETTLED or
 *  VOID that is on or after it is worked out again over the holidays now (see {@link ClaimRepository#refixDueBy}),
 *  and the figures that moved are recorded after HOLIDAY_ADDED. */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate holiday, ServerHttpConnection connection,
            SettingsRepository repository, ClaimRepository claims, Rollup rollup, Audit audit,
            Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.holidays(id).contains(holiday.toString())) {
            throw new BadRequestException(holiday + " is already a holiday");
        }
        Rollup.Snapshot before = rollup.take(id);
        java.util.Set<String> holidays = new java.util.HashSet<>(repository.holidays(id));
        repository.addHoliday(id, holiday);
        claims.refixDueBy(id, holidays, holiday);
        audit.record("HOLIDAY_ADDED sheet=" + id + " by=" + by + " date=" + holiday);
        rollup.record(id, by, before);
        settings.set(repository.load(id));
    }
}
