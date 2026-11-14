package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the pending SLA just saved (blank: none), even when it is the one the sheet had; nothing when left as it is. */
public class AuditPendingSla {
    public void audit(@Val PendingSla days, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!days.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=pending-sla value="
                + (days.value() == null ? "" : days.value().toString()));
    }
}
