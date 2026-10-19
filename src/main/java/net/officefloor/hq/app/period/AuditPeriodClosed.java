package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the closed period and the due-total it was closed at. */
public class AuditPeriodClosed {
    public void audit(@Val ClosedPeriod closed, @RequestParam(value = "user", required = false) String user, Audit audit) {
        Period period = closed.period();
        audit.record("PERIOD_CLOSED sheet=" + period.sheetId() + " by=" + user
                + " period=" + period.name() + " total=" + period.closedTotal().toPlainString());
    }
}
