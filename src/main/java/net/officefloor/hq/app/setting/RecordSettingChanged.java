package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a change that went through is recorded (with its from-date when one was given). */
public class RecordSettingChanged {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Setting setting, @Val SettingRequest request, Audit audit) {
        audit.record("SETTING_CHANGED sheet=" + setting.sheetId() + " by=" + (by == null ? "" : by)
                + " key=" + setting.key() + " value=" + setting.value()
                + (request.from() == null ? "" : " from=" + request.from()));
    }
}
