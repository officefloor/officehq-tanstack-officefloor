package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Soft-closes an open period: its due-total and each person's are fixed as a close fixes them (see ClosePeriod), but
 *  claims dated inside it can still be added, edited, opened, settled and voided. Soft-closing a period closed or
 *  soft-closed already is refused, and so is soft-closing by a role the sheet's close permission does not allow. */
public class SoftClosePeriod {
    public void close(@Val Period period, @Val PeriodCloseRequest request, PeriodCloser closer,
            SettingRepository settings, RoleRepository roles, Out<PeriodClose> closed)
            throws InvalidPeriodException {
        if (!settings.permits(period.sheetId(), "close", roles.roleOf(period.sheetId(), request.by()))) {
            throw new InvalidPeriodException("The sheet's permission does not let this user close a period");
        }
        closed.set(new PeriodClose(request.by(), closer.softClose(period)));
    }
}
