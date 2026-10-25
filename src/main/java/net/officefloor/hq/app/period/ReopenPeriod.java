package net.officefloor.hq.app.period;

import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reopens a closed or soft-closed period, so its due-total is live again; only a role the sheet's reopen permission gives may (a steward while blank), and a period still open is refused.
 */
public class ReopenPeriod {
    public void reopen(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            SettingRepository settings, Out<ReopenedPeriod> reopened) throws BadRequestException {
        String role = roles.roleOf(period.sheetId(), by.name());
        if (!settings.allows(period.sheetId(), "reopen", role)) {
            throw new BadRequestException("A " + role + " may not reopen a period on this sheet");
        }
        if (!period.closed() && !period.soft()) {
            throw new BadRequestException("That period is not closed");
        }
        if (!repository.reopen(period.id())) {
            throw new BadRequestException("The period was reopened by someone else; reload the sheet");
        }
        reopened.set(new ReopenedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(),
                period.to(), false, null, Map.of(), false)));
    }
}
