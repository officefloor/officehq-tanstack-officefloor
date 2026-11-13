package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the cap just saved (blank: no cap), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditCap {
    public void audit(@Val Cap cap, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!cap.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=cap value="
                + (cap.value() == null ? "" : cap.value().toPlainString()));
    }
}
