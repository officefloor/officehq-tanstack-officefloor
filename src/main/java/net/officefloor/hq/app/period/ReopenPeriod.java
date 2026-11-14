package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Reopens a closed period: its due-total is live again. Refused for anyone but a steward, and when
 *  the period is not closed. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            RoleRepository roles, Out<Period> period) throws InvalidPeriodException, PeriodReopenException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Role.STEWARD.equals(roles.roleOf(id, by == null ? "" : by))) {
            throw new PeriodReopenException("Only a steward can reopen a period");
        }
        if (!found.closed()) {
            throw new PeriodReopenException("The period is not closed");
        }
        period.set(periods.reopen(found));
    }
}
