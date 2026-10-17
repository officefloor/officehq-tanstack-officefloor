package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERIOD_ADDED once the period is saved; the signed-in user arrives as X-User. */
public class AuditPeriodAdded {
    public void audit(@Val Period period, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " period=" + period.getName() + " from=" + period.getFromDate() + " to=" + period.getToDate());
    }
}
