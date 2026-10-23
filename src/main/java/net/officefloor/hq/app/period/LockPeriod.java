package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Lock the named period so it cannot be reopened. Refused when the user is not a steward on the sheet, there is no
 *  such period, or it is already locked. */
public class LockPeriod {
    public void lock(@PathVariable("sheetId") String sheetId, @PathVariable("periodName") String periodName,
            @Val PeriodCloseInput input, PeriodRepository periods, RoleRepository roles, Out<Period> locked)
            throws InvalidPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        if (!Role.STEWARD.equals(roles.of(id, input.user()))) {
            throw new InvalidPeriodException("Only a steward may lock a period on this sheet.");
        }
        Period period = periods.find(id, periodName);
        if (period == null) {
            throw new InvalidPeriodException("There is no period " + periodName.trim() + " on this sheet.");
        }
        if (!periods.setLocked(period, true)) {
            throw new InvalidPeriodException("The period " + period.name() + " is already locked.");
        }
        locked.set(period);
    }
}
