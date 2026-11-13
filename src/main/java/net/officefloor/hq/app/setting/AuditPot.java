package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the pot just saved (blank: none), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditPot {
    public void audit(@Val Pot pot, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!pot.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=pot value="
                + (pot.value() == null ? "" : pot.value().toPlainString()));
    }
}
