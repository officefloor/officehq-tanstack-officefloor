package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the maximum of open claims per person just saved (blank: none), even when it is the one the sheet had; nothing when left as it is. */
public class AuditMaxOpen {
    public void audit(@Val MaxOpen max, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!max.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=max-open value="
                + (max.value() == null ? "" : max.value().toString()));
    }
}
