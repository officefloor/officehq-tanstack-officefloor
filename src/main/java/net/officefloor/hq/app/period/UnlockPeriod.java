package net.officefloor.hq.app.period;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Unlocks a locked period, so it can be reopened again. Only a steward may; refuses a period not locked. */
public class UnlockPeriod {
    public void unlock(@Val Period period, @RequestHeader(name = "X-User", required = false) String user,
            PeriodRepository repository, RoleRepository roles) throws InvalidPeriodException {
        String role = roles.roleOf(period.getSheetId(), AuditUser.decode(user));
        if (!Role.STEWARD.equals(role)) {
            throw new InvalidPeriodException("A " + role + " cannot unlock a period.");
        }
        if (!period.isLocked()) {
            throw new InvalidPeriodException("The period is not locked.");
        }
        period.setLocked(false);
        repository.saveAndFlush(period);
    }
}
