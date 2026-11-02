package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Unlocks a locked period so it may be reopened again. Only a steward may; refuses anyone else, a period not on the sheet, or one not locked. */
public class UnlockPeriod {
    public void unlock(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, Roles roles, Audit audit)
            throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPeriodException("Only a steward may unlock a period");
        }
        if (!periods.named(sheet, period)) {
            throw new InvalidPeriodException("There is no period named " + period);
        }
        if (!periods.lock(sheet, period, false)) {
            throw new InvalidPeriodException("The period " + period + " is not locked");
        }
        audit.record("PERIOD_UNLOCKED sheet=" + sheet + " by=" + user + " period=" + period);
    }
}
