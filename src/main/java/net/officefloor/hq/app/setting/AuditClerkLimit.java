package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the clerk limit just saved (blank: no limit), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditClerkLimit {
    public void audit(@Val ClerkLimit limit, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!limit.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=clerk-limit value="
                + (limit.value() == null ? "" : limit.value().toPlainString())
                + DatedSettings.auditFrom(limit.from()));
    }
}
