package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.claim.ClaimLogRepository;
import net.officefloor.hq.app.claim.ClaimRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.setting.SettingsRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Soft-closes an open period: its due-total and each person's figure are fixed as a close fixes them,
 * but claims dated inside it can still be added, edited, opened, settled and voided. The same roles
 * as may close may soft-close; a period already closed or soft-closed is refused.
 */
public class SoftClosePeriod {
    public void close(@Val Sheet sheet, @Val Period period, PeriodRepository periods, PersonRepository people,
            ClaimRepository claims, ClaimLogRepository log, RateRepository rates, SettingsRepository settings, RoleRepository roles,
            HttpServletRequest request, Out<Period> closedOut) throws InvalidPeriodException {
        String role = roles.roleOf(sheet.id(), Audit.user(request));
        if (!settings.find(sheet.id()).allows("close", role)) {
            throw new InvalidPeriodException("A " + role + " may not soft-close period " + period.name());
        }
        if (period.closed()) throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        Period closed = ClosePeriod.fix(sheet, period, true, periods, people, claims, log, rates, settings);
        if (closed == null) throw new InvalidPeriodException("Period " + period.name() + " is already closed");
        closedOut.set(closed);
    }
}
