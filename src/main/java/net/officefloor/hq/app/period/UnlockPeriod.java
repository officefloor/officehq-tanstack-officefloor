package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Unlocks a period. Refused for anyone but a steward, and when it is not locked. */
public class UnlockPeriod {
    public void unlock(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            RoleRepository roles, Out<Period> period) throws InvalidPeriodException, PeriodLockException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Role.STEWARD.equals(roles.roleOf(id, by == null ? "" : by))) {
            throw new PeriodLockException("Only a steward can unlock a period");
        }
        if (found.locked() == false) {
            throw new PeriodLockException("The period is not locked");
        }
        period.set(periods.lock(found, false));
    }
}
