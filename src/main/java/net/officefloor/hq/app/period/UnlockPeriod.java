package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Unlocks the period; only a steward may, and a period not locked is refused.
 */
public class UnlockPeriod {
    public void unlock(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            Out<LockedPeriod> unlocked) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(period.sheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may unlock a period");
        }
        if (!period.locked() || !repository.setLocked(period.id(), false)) {
            throw new BadRequestException("That period is not locked");
        }
        unlocked.set(new LockedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(), period.to(),
                period.closed(), period.closedTotal(), period.personTotals(), period.soft(), period.cap(),
                period.personCarried(), false)));
    }
}
