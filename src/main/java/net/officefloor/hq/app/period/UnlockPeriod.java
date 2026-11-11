package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Unlocks a locked period, so it can be reopened again. The same roles as may lock may unlock; a
 * period not locked is refused.
 */
public class UnlockPeriod {
    public void unlock(@Val Sheet sheet, @Val Period period, PeriodRepository periods, RoleRepository roles,
            SettingsRepository settings, HttpServletRequest request, Out<Period> unlockedOut) throws InvalidPeriodException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("lock", role)) {
            throw new InvalidPeriodException("A " + role + " may not unlock period " + period.name());
        }
        if (!period.locked() || !periods.markLocked(period, false)) {
            throw new InvalidPeriodException("Period " + period.name() + " is not locked");
        }
        unlockedOut.set(period.withLocked(false));
    }
}
