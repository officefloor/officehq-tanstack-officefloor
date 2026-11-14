package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Locks the period. Refused (400) when the user does not act as a steward or the period is already locked. */
public class LockPeriod {
    public void lock(@Val Period period, @RequestParam("user") String user, SettingRepository settings,
            Out<Period> locked) throws InvalidPeriodException {
        if (settings.find(period.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPeriodException("Only a steward may lock a period.");
        }
        if (period.locked() == true) {
            throw new InvalidPeriodException("The period " + period.name() + " is already locked.");
        }
        locked.set(period.withLocked(true));
    }
}
