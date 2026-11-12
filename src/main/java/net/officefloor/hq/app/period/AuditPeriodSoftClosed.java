package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a soft-closed period: PERIOD_SOFT_CLOSED sheet=<id> by=<user> period=<name> total=<0.00>. */
public class AuditPeriodSoftClosed {
    public void audit(@PathVariable("sheetId") String sheetId, @Val CloseRequest request, @Val PeriodResponse period,
            Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + sheetId.trim() + " by=" + request.user() + " period=" + period.name()
                + " total=" + period.total());
    }
}
