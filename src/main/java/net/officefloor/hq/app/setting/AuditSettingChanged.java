package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the sheet's clerk limit, once it is saved (blank when there is none). */
public class AuditSettingChanged {
    public void audit(@Val Sheet sheet, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        String limit = SettingsResponse.from(sheet).clerkLimit();
        audit.record("SETTING_CHANGED sheet=" + sheet.getId() + " by=" + AuditUser.decode(user)
                + " key=clerk-limit value=" + (limit == null ? "" : limit));
    }
}
