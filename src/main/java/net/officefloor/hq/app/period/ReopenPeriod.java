package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reopens the period, making its due-total live again. Refused (400) when the user is not a steward or the period
 * is not closed.
 */
public class ReopenPeriod {
    public void reopen(@Val Period period, @RequestParam("user") String user, SettingRepository settings,
            Out<Period> reopened) throws InvalidPeriodException {
        if (settings.find(period.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPeriodException("Only a steward can reopen a period.");
        }
        if (!period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed.");
        }
        reopened.set(period.reopen());
    }
}
