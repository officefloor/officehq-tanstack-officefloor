package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Soft-closes the period (by a role the sheet's close permission gives), fixing its due-total and each person's as
 * they are now while claims dated inside it can still change; refuses a period already closed or soft-closed.
 */
public class SoftClosePeriod {
    public void softClose(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            SettingRepository settings, Rollup rollup, Out<SoftClosedPeriod> softClosed) throws BadRequestException {
        String role = roles.roleOf(period.sheetId(), by.name());
        if (!settings.allows(period.sheetId(), "close", role)) {
            throw new BadRequestException("A " + role + " may not close a period on this sheet");
        }
        if (period.closed()) {
            throw new BadRequestException("That period is already closed");
        }
        if (period.soft()) {
            throw new BadRequestException("That period is already soft-closed");
        }
        Period fixed = ClosePeriod.softFix(period, repository, rollup);
        if (fixed == null) {
            throw new BadRequestException("The period was closed by someone else; reload the sheet");
        }
        softClosed.set(new SoftClosedPeriod(fixed));
    }
}
