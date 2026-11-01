package net.officefloor.hq.app.period;

import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Soft-closes the period: its due-total and each person's are fixed now as a close fixes them, but claims dated in it
 * may still be added, edited and change status (one added meanwhile is a late entry). Refuses a role the permission
 * matrix does not let close, a period not on the sheet, or one already closed or soft-closed.
 */
public class SoftClosePeriod {
    public void softClose(@PathVariable("sheet") String sheet, @PathVariable("period") String period,
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
        if (found.fixed() || !closer.close(sheet, user, found, false, true)) {
            throw new InvalidPeriodException("The period " + period + " is already closed");
        }
    }
}
