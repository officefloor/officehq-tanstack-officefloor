package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reopens a closed period, making its due-total live again. Only a steward may reopen; a period that
 * is not closed is refused.
 */
public class ReopenPeriod {
    public void reopen(@Val Sheet sheet, @Val Period period, PeriodRepository periods, RoleRepository roles,
            HttpServletRequest request, Out<Period> reopenedOut) throws InvalidPeriodException {
        String user = Audit.user(request);
        if (!Role.STEWARD.equals(roles.roleOf(sheet.id(), user))) {
            throw new InvalidPeriodException("Only a steward may reopen period " + period.name());
        }
        if (!period.closed()) throw new InvalidPeriodException("Period " + period.name() + " is not closed");
        Period reopened = period.reopen();
        if (!periods.markReopened(reopened)) {
            throw new InvalidPeriodException("Period " + period.name() + " is not closed");
        }
        periods.deletePersonTotals(reopened);
        reopenedOut.set(reopened);
    }
}
