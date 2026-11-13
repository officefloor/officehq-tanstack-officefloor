package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the period just soft-closed, with the due-total it fixed. */
public class AuditPeriodSoftClosed {
    public void audit(@Val Period period, @RequestParam("user") String user, Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.sheetId() + " by=" + user + " period=" + period.name()
                + " total=" + period.closedTotal().setScale(2).toPlainString());
    }
}
