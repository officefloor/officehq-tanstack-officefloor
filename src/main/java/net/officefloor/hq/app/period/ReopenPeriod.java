package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reopens a closed period, making its due-total, and each person's figure and carried figure, live again. Only a role the permission matrix allows (by
 * the standing rule, a steward) may reopen; a period that
 * is not closed is refused.
 */
public class ReopenPeriod {
    public void reopen(@Val Sheet sheet, @Val Period period, PeriodRepository periods, RoleRepository roles, SettingsRepository settings,
            HttpServletRequest request, Out<Period> reopenedOut) throws InvalidPeriodException {
        String user = Audit.user(request);
        String role = roles.roleOf(sheet.id(), user);
        if (!settings.find(sheet.id()).allows("reopen", role)) {
            throw new InvalidPeriodException("A " + role + " may not reopen period " + period.name());
        }
        if (!period.closed()) throw new InvalidPeriodException("Period " + period.name() + " is not closed");
        Period reopened = period.reopen();
        if (!periods.markReopened(reopened)) {
            throw new InvalidPeriodException("Period " + period.name() + " is not closed");
        }
        periods.deletePersonTotals(reopened);
        periods.deletePersonCarried(reopened);
        reopenedOut.set(reopened);
    }
}
