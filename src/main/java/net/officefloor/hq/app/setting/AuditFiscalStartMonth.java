package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the fiscal year start month just saved, even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditFiscalStartMonth {
    public void audit(@Val FiscalStartMonth month, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!month.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=fiscal-start-month value=" + month.value());
    }
}
