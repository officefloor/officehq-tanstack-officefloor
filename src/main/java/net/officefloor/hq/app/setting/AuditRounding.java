package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the rounding mode just saved (blank: none), even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditRounding {
    public void audit(@Val RoundingSetting rounding, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (!rounding.given()) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=rounding value="
                + (rounding.value() == null ? "" : rounding.value())
                + DatedSettings.auditFrom(rounding.from()));
    }
}
