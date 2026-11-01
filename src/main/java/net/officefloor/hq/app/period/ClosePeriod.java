package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes the period, fixing its due-total now, and each person's on the sheet (the part of it from their claims): the due now of the claims dated in it
 * that counted at the end of its last day (OPEN, PENDING and SETTLED then by their status log, but not a deferred one, nor an unbound open one dated
 * within its person's hold) (a
 * claim with no rate adds nothing). Refuses a role the permission matrix does not let close, a period not on the sheet or one already closed.
 */
public class ClosePeriod {
    public void close(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
            @RequestParam("user") String user, Periods periods, PeriodCloser closer, Settings settings,
            Roles roles)
            throws InvalidPeriodException {
        if (!settings.permits(sheet, "close", roles.of(sheet, user))) {
            throw new InvalidPeriodException("Your role may not close a period");
        }
        Period found = periods.find(sheet, period);
        if (found == null) {
            throw new InvalidPeriodException("There is no period named " + period);
        }
        if (found.closed()) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
        if (!closer.close(sheet, user, found, false)) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
    }
}
