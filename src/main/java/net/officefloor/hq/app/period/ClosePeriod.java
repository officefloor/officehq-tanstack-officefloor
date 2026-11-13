package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Closes the period, fixing its due-total as it stands now (see {@link PeriodCloser}). Refused (400) when it is
 * already closed, or when the permission matrix does not allow the user's role closing.
 */
public class ClosePeriod {
    public void close(@Val Period period, PeriodCloser closer, SettingRepository settings,
            @RequestParam("user") String user, Out<Period> closed) throws InvalidPeriodException {
        if (!settings.find(period.sheetId()).may(user, Permissions.CLOSE)) {
            throw new InvalidPeriodException("The user's role is not allowed to close a period.");
        }
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        closed.set(closer.close(period));
    }
}
