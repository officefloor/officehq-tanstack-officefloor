package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the floor just saved, even when it is the one the sheet had; nothing when it was left as it is. */
public class AuditFloor {
    public void audit(@Val Floor floor, @Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        if (floor.value() == null) {
            return;
        }
        audit.record("SETTING_CHANGED sheet=" + sheet.id() + " by=" + user + " key=floor value="
                + floor.value().toPlainString()
                + DatedSettings.auditFrom(floor.from()));
    }
}
