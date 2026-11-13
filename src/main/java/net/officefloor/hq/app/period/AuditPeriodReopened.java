package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the period just reopened. */
public class AuditPeriodReopened {
    public void audit(@Val Period period, @RequestParam("user") String user, Audit audit) {
        audit.record("PERIOD_REOPENED sheet=" + period.sheetId() + " by=" + user + " period=" + period.name());
    }
}
