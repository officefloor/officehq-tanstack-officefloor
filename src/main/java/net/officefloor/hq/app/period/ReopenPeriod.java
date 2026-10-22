package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Reopen the named period, dropping its fixed due-total so it is live again. Refused when the user's role may
 *  not reopen (the sheet's permission matrix; by default a steward only), there is no such period, or it is not closed. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val PeriodCloseInput input, PeriodRepository periods, RoleRepository roles, SettingRepository settings,
            Out<Period> reopened) throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (!settings.permissions(id).allows("reopen", roles.of(id, input.user()))) {
            throw new InvalidPeriodException("Your role may not reopen a period on this sheet.");
        }
        Period period = periods.find(id, periodName);
        if (period == null) {
            throw new InvalidPeriodException("There is no period " + periodName.trim() + " on this sheet.");
        }
        if (!period.closed() || !periods.reopen(period)) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed.");
        }
        reopened.set(new Period(period.sheetId(), period.name(), period.from(), period.to(), false, null));
    }
}
