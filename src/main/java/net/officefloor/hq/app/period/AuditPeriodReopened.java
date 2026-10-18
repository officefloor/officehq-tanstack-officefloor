package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records PERIOD_REOPENED once the period is saved open; the signed-in user arrives as X-User. */
public class AuditPeriodReopened {
    public void audit(@Val Period period, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record("PERIOD_REOPENED sheet=" + period.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " period=" + period.getName());
    }
}
