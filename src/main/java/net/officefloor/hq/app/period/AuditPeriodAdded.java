package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the added period. Runs after every rule has passed, so a refused period writes nothing. */
public class AuditPeriodAdded {
    public void audit(@Val Sheet sheet, @Val Period period, HttpServletRequest request, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to()
                + (period.cap() == null ? "" : " cap=" + period.cap()));
    }
}
