package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes SETTING_CHANGED to the audit file once the setting is stored (value= blank for none; from=<date> at its
 *  end when a from-date was given). Then writes the figures the change moved (see Rollup). */
public class RecordSettingChanged {
    public void record(@Val Setting setting, @Val SettingValues values, @Val RollupBefore before, Rollup rollup,
            Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + setting.sheetId() + " by=" + values.by() + " key=" + setting.key()
                + " value=" + (setting.value() == null ? "" : setting.value())
                + (values.dated() && values.from() != null ? " from=" + values.from() : ""));
        rollup.record(setting.sheetId(), values.by(), before);
    }
}
