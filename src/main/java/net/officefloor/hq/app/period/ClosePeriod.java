package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes an open period, fixing its due-total as it is now: the figures (each due as shown now, with its late charge)
 *  of the claims whose date falls in it and that counted by the status each had at the end of the period's last day,
 *  as its status log records it (a claim added after it does not count), the live rules otherwise applying (a
 *  person's claims OPEN or PENDING now and dated within a hold of theirs, not bound by a statement, do not, nor do deferred
 *  claims), added up (skipping n/a). Each person's own due-total for the period is fixed
 *  too, for every person on the sheet; the period's is the sum of theirs. Closing a period already closed is refused, and so is closing by a role the sheet's close permission does not allow. */
public class ClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, PeriodCloser closer,
            SettingRepository settings, RoleRepository roles, Out<PeriodClose> closed)
            throws InvalidPeriodException {
        if (!settings.permits(period.sheetId(), "close", roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("The sheet's permission does not let this user close a period");
        }
        closed.set(new PeriodClose(request.by(), closer.close(period)));
    }
}
