package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Soft-closes the period, fixing its due-total and each person's figure as they stand now (see {@link PeriodCloser}),
 * while claims dated in it can still be added and changed. Refused (400) when it is already closed or soft-closed, or
 * when the permission matrix does not allow the user's role closing.
 */
public class SoftClosePeriod {
    public void softClose(@Val Period period, PeriodCloser closer, SettingRepository settings,
            @RequestParam("user") String user, Out<Period> closed) throws InvalidPeriodException {
        if (!settings.find(period.sheetId()).may(user, Permissions.CLOSE)) {
            throw new InvalidPeriodException("The user's role is not allowed to close a period.");
        }
        if (period.fixed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        closed.set(closer.softClose(period));
    }
}
