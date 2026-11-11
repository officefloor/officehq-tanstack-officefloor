package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the unlocked period. */
public class AuditPeriodUnlocked {
    public void audit(@Val Sheet sheet, @Val Period period, HttpServletRequest request, Audit audit) {
        audit.record("PERIOD_UNLOCKED sheet=" + sheet.id() + " by=" + Audit.user(request) + " period=" + period.name());
    }
}
