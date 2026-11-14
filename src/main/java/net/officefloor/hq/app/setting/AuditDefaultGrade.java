package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the default grade just saved (blank: A), even when it is the one the sheet had; nothing when left as it is. */
public class AuditDefaultGrade {
    public void audit(@Val DefaultGrade grade, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!grade.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=default-grade value="
                + (grade.value() == null ? "" : grade.value()));
    }
}
