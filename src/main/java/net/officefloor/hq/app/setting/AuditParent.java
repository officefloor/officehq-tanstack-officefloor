package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the parent sheet just saved (blank: none), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditParent {
    public void audit(@Val Parent parent, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!parent.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=parent value="
                + (parent.value() == null ? "" : parent.value()));
    }
}
