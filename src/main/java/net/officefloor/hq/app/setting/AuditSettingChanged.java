package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored setting: SETTING_CHANGED sheet=<id> by=<user> key=<key> value=<value> (an empty
 *  value when blank). */
public class AuditSettingChanged {
    public void audit(@PathVariable("sheetId") String sheetId, @Val SettingInput setting, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + sheetId.trim() + " by=" + setting.user() + " key=" + setting.key()
                + " value=" + (setting.value() == null ? "" : setting.value()));
    }
}
