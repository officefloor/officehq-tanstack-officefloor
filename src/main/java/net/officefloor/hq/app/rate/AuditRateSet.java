package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the saved rate. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditRateSet {
    public void audit(@Val Sheet sheet, @Val Rate rate, HttpServletRequest request, Audit audit) {
        audit.record("RATE_SET sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " grade=" + rate.grade() + " rate=" + rate.pct().toPlainString()
                + (rate.from() == null ? "" : " from=" + rate.from())
                + (rate.to() == null ? "" : " to=" + rate.to()));
    }
}
