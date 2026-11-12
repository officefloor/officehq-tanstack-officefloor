package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored setting: SETTING_CHANGED sheet=<id> by=<user> key=<key> value=<value> (an empty
 *  value when blank), then from=<date> when a from-date was given, then how the change moved the sheet's figures (the floor moves live dues). */
public class AuditSettingChanged {
    public void audit(@PathVariable("sheetId") String sheetId, @Val SettingInput setting, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("SETTING_CHANGED sheet=" + sheetId.trim() + " by=" + setting.user() + " key=" + setting.key()
                + " value=" + (setting.value() == null ? "" : setting.value())
                + (setting.from() == null ? "" : " from=" + setting.from()));
        rollups.record(sheetId.trim(), setting.user(), before);
    }
}
