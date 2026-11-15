package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Locks a period. Refused for anyone but a steward, and when it is already locked. */
public class LockPeriod {
    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            RoleRepository roles, Out<Period> period) throws InvalidPeriodException, PeriodLockException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Role.STEWARD.equals(roles.roleOf(id, by == null ? "" : by))) {
            throw new PeriodLockException("Only a steward can lock a period");
        }
        if (found.locked() == true) {
            throw new PeriodLockException("The period is already locked");
        }
        period.set(periods.lock(found, true));
    }
}
