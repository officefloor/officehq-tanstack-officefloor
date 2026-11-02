package net.officefloor.hq.app.settings;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add a date to the sheet's holidays; one already on the list is refused. */
public class SaveHoliday {
    public void save(@PathVariable("sheetId") String sheetId, @Val LocalDate holiday, ServerHttpConnection connection,
            SettingsRepository repository, Audit audit, Out<Settings> settings) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        if (repository.holidays(id).contains(holiday.toString())) {
            throw new BadRequestException(holiday + " is already a holiday");
        }
        repository.addHoliday(id, holiday);
        audit.record("HOLIDAY_ADDED sheet=" + id + " by=" + by + " date=" + holiday);
        settings.set(repository.load(id));
    }
}
