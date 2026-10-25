package net.officefloor.hq.app.period;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Locks the period, so it cannot be reopened. Only a steward may; refuses a period already locked. */
public class LockPeriod {
    public void lock(@Val Period period, @RequestHeader(name = "X-User", required = false) String user,
            PeriodRepository repository, RoleRepository roles) throws InvalidPeriodException {
        String role = roles.roleOf(period.getSheetId(), AuditUser.decode(user));
        if (!Role.STEWARD.equals(role)) {
            throw new InvalidPeriodException("A " + role + " cannot lock a period.");
        }
        if (period.isLocked()) {
            throw new InvalidPeriodException("The period is already locked.");
        }
        period.setLocked(true);
        repository.saveAndFlush(period);
    }
}
