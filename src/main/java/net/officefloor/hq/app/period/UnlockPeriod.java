package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Role;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Unlocks the period. Refused (400) when the user does not act as a steward or the period is already unlocked. */
public class UnlockPeriod {
    public void unlock(@Val Period period, @RequestParam("user") String user, SettingRepository settings,
            Out<Period> unlocked) throws InvalidPeriodException {
        if (settings.find(period.sheetId()).roleOf(user) != Role.STEWARD) {
            throw new InvalidPeriodException("Only a steward may unlock a period.");
        }
        if (period.locked() == false) {
            throw new InvalidPeriodException("The period " + period.name() + " is already unlocked.");
        }
        unlocked.set(period.withLocked(false));
    }
}
