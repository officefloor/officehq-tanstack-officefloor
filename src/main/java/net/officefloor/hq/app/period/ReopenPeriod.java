package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.UserRole;
import net.officefloor.hq.app.setting.UserRoleRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Reopens the period, refusing one not closed or a user (arriving as X-User) who is not a steward; its
 *  due-total is live again. */
public class ReopenPeriod {
    public void reopen(@Val Period period, @HttpHeaderParameter("X-User") String user, UserRoleRepository roles)
            throws InvalidPeriodException {
        String by = user == null ? "" : user.trim();
        if (!UserRole.STEWARD.equals(roles.roleOf(period.getSheetId(), by))) {
            throw new InvalidPeriodException("Only a steward can reopen a period.");
        }
        if (!period.isClosed()) {
            throw new InvalidPeriodException("The period " + period.getName() + " is not closed.");
        }
        period.reopen();
    }
}
