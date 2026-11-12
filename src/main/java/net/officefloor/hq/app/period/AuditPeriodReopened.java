package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a reopened period: PERIOD_REOPENED sheet=<id> by=<user> period=<name>. */
public class AuditPeriodReopened {
    public void audit(@PathVariable("sheetId") String sheetId, @Val CloseRequest request, @Val PeriodResponse period,
            Audit audit) {
        audit.record("PERIOD_REOPENED sheet=" + sheetId.trim() + " by=" + request.user() + " period=" + period.name());
    }
}
