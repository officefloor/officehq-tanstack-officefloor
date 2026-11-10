package net.officefloor.hq.app.period;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the soft-closed period and the due-total it fixed. */
public class AuditPeriodSoftClosed {
    public void audit(@Val Sheet sheet, @Val Period period, HttpServletRequest request, Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " period=" + period.name() + " total=" + period.closedTotal().setScale(2).toPlainString());
    }
}
