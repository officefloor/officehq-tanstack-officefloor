package net.officefloor.hq.app.access;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordSettingChanged {
    public void record(@Val Setting setting, @Val ChangedBy by, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + setting.sheetId() + " by=" + by.name() + " key=" + setting.key()
                + " value=" + (setting.value() == null ? "" : setting.value()));
    }
}
