package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the terms just saved, even when they are the ones the sheet had; nothing when they were left as they are. */
public class AuditTerms {
    public void audit(@Val Terms terms, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (terms.value() == null) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=terms value=" + terms.value());
    }
}
