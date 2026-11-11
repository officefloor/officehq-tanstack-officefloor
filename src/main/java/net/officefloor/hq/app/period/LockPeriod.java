package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Locks a period, so it cannot be reopened. Only a role the permission matrix allows (by the
 * standing rule, a steward) may lock; a period already locked is refused.
 */
public class LockPeriod {
    public void lock(@Val Sheet sheet, @Val Period period, PeriodRepository periods, RoleRepository roles,
            SettingsRepository settings, HttpServletRequest request, Out<Period> lockedOut) throws InvalidPeriodException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("lock", role)) {
            throw new InvalidPeriodException("A " + role + " may not lock period " + period.name());
        }
        if (period.locked() || !periods.markLocked(period, true)) {
            throw new InvalidPeriodException("Period " + period.name() + " is already locked");
        }
        lockedOut.set(period.withLocked(true));
    }
}
