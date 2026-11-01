package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reopens a closed period: its fixed due-total is dropped, so it totals its claims' current figures again. Only a
 * steward may; refuses anyone else, a period not on the sheet, or one not closed.
 */
public class ReopenPeriod {
    public void reopen(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, Roles roles, Audit audit)
            throws InvalidPeriodException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPeriodException("Only a steward may reopen a period");
        }
        Period found = periods.find(sheet, period);
        if (found == null) {
            throw new InvalidPeriodException("There is no period named " + period);
        }
        if (!found.closed() || !periods.reopen(sheet, period)) {
            throw new InvalidPeriodException("The period " + period + " is not closed");
        }
        audit.record("PERIOD_REOPENED sheet=" + sheet + " by=" + user + " period=" + period);
    }
}
