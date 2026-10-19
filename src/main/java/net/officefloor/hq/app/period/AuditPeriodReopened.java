package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the reopened period. */
public class AuditPeriodReopened {
    public void audit(@Val ReopenedPeriod reopened, @RequestParam(value = "user", required = false) String user, Audit audit) {
        Period period = reopened.period();
        audit.record("PERIOD_REOPENED sheet=" + period.sheetId() + " by=" + user + " period=" + period.name());
    }
}
