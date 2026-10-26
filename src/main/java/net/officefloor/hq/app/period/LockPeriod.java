package net.officefloor.hq.app.period;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.Role;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Locks the period, so it cannot be reopened until unlocked; only a steward may, and a period already locked is refused.
 */
public class LockPeriod {
    public void lock(@Val Period period, @Val ChangedBy by, PeriodRepository repository, RoleRepository roles,
            Out<LockedPeriod> locked) throws BadRequestException {
        if (!Role.STEWARD.equals(roles.roleOf(period.sheetId(), by.name()))) {
            throw new BadRequestException("Only a steward may lock a period");
        }
        if (period.locked() || !repository.setLocked(period.id(), true)) {
            throw new BadRequestException("That period is already locked");
        }
        locked.set(new LockedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(), period.to(),
                period.closed(), period.closedTotal(), period.personTotals(), period.soft(), period.cap(),
                period.personCarried(), true)));
    }
}
