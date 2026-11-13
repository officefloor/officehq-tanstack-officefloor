package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the grace days just saved (blank: off), even when they are the ones the sheet had; nothing when left as they are. */
public class AuditGraceDays {
    public void audit(@Val GraceDays days, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!days.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=grace-days value="
                + (days.value() == null ? "" : days.value().toString()));
    }
}
