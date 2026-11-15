package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Reopens a closed or soft-closed period: its due-total is live again. Refused for a role the permission matrix does not let reopen (by default
 *  anyone but a steward), and when
 *  the period is not closed or is locked. */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name,
            @RequestParam(name = "by", required = false) String by, PeriodRepository periods,
            RoleRepository roles, SettingRepository settings, Out<Period> period) throws InvalidPeriodException, PeriodReopenException {
        String id = sheetId.trim();
        Period found = periods.find(id, name);
        if (found == null) {
            throw new InvalidPeriodException("No such period: " + name);
        }
        if (!Permissions.allows(id, by, Permissions.REOPEN, roles, settings)) {
            throw new PeriodReopenException("Your role may not reopen a period");
        }
        if (found.locked()) {
            throw new PeriodReopenException("The period is locked");
        }
        if (!found.fixed()) {
            throw new PeriodReopenException("The period is not closed");
        }
        period.set(periods.reopen(found));
    }
}
