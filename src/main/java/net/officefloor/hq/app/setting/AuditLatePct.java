package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the late percent just saved, even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditLatePct {
    public void audit(@Val LatePct latePct, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (latePct.value() == null) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=late-pct value="
                + latePct.value().toPlainString()
                + DatedSettings.auditFrom(latePct.from()));
    }
}
