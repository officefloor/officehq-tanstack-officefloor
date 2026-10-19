package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the setting once it is valid and stored. */
public class AuditSettingChanged {
    public void audit(@Val Setting setting, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + setting.sheetId() + " by=" + user
                + " key=" + setting.key() + " value=" + setting.value());
    }
}
