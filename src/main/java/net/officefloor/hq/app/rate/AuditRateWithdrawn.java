package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the withdrawn rate. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditRateWithdrawn {
    public void audit(@Val Sheet sheet, @Val WithdrawnRate rate, HttpServletRequest request, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " grade=" + rate.grade() + (rate.from() == null ? "" : " from=" + rate.from()));
    }
}
