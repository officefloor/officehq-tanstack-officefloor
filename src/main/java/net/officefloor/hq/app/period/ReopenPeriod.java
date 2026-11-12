package net.officefloor.hq.app.period;

import java.time.LocalDate;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Reopens a closed or soft-closed period, dropping its fixed due-total so it is live again. Refused when the sheet's
 *  permission matrix does not allow the user's role to reopen (by default only a steward), there is no such period, it is neither closed nor soft-closed, or it is locked. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val CloseRequest request, PeriodStore store, RoleStore roles, SettingStore settings,
            Out<PeriodResponse> reopened) throws InvalidSheetException {
        String sheet = sheetId.trim();
        Role role = roles.find(sheet, request.user());
        if (!settings.allows(sheet, "reopen", role)) {
            throw new InvalidSheetException("A " + role.shown() + " may not reopen a period");
        }
        PeriodResponse period = store.find(sheet, periodName.trim());
        if (period == null) {
            throw new InvalidSheetException("No such period: " + periodName.trim());
        }
        if (period.locked()) {
            throw new InvalidSheetException("The period " + period.name() + " is locked");
        }
        if (!(period.closed() || period.soft()) || !store.reopen(sheet, period.name())) {
            throw new InvalidSheetException("The period " + period.name() + " is not closed");
        }
        reopened.set(PeriodResponse.of(period.name(), LocalDate.parse(period.from()), LocalDate.parse(period.to()), period.cap()));
    }
}
