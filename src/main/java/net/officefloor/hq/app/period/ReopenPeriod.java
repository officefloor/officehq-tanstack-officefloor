package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Val;

/** Reopens a closed period, so its due-total is live again. Only a role the sheet's reopen permission allows (a steward while it is blank) may reopen; reopening a period that is
 *  not closed is refused. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @Val PeriodCloseRequest request, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings) throws InvalidPeriodException {
        if (!settings.permits(period.sheetId(), "reopen", roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("The sheet's permission does not let this user reopen a period");
        }
        if (!period.closed() || !periods.reopen(period)) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed");
        }
    }
}
