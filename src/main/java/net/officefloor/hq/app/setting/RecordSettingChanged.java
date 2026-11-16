package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes SETTING_CHANGED to the audit file once the setting is stored (value= blank for none). */
public class RecordSettingChanged {
    public void record(@Val Setting setting, @Val SettingValues values, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + setting.sheetId() + " by=" + values.by() + " key=" + setting.key()
                + " value=" + (setting.value() == null ? "" : setting.value()));
    }
}
