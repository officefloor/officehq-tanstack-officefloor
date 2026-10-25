package net.officefloor.hq.app.period;

import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Reopens a closed period, so its due-total is live again; only a steward may, and a period still open is refused.
 */
public class ReopenPeriod {
    public void reopen(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            Out<ReopenedPeriod> reopened) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(period.sheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may reopen a period");
        }
        if (!period.closed()) {
            throw new BadRequestException("That period is not closed");
        }
        if (!repository.reopen(period.id())) {
            throw new BadRequestException("The period was reopened by someone else; reload the sheet");
        }
        reopened.set(new ReopenedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(),
                period.to(), false, null, Map.of())));
    }
}
