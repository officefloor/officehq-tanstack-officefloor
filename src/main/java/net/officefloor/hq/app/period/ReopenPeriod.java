package net.officefloor.hq.app.period;

import net.officefloor.hq.app.setting.Permissions;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reopens the period, making its due-total live again. Refused (400) when the permission matrix does not allow the user's role reopening (standing rule: a steward) or the period
 * is neither closed nor soft-closed, or is locked.
 */
public class ReopenPeriod {
    public void reopen(@Val Period period, @RequestParam("user") String user, SettingRepository settings,
            Out<Period> reopened) throws InvalidPeriodException {
        if (!settings.find(period.sheetId()).may(user, Permissions.REOPEN)) {
            throw new InvalidPeriodException("The user's role is not allowed to reopen a period.");
        }
        if (!period.fixed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is not closed.");
        }
        if (period.locked()) {
            throw new InvalidPeriodException("The period " + period.name() + " is locked.");
        }
        reopened.set(period.reopen());
    }
}
