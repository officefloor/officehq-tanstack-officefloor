package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Saves the setting; a floor moves the live dues, so the figures it moved are audited after it. */
public class SaveSetting {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val SettingInput input,
            Settings settings, Audit audit, Rollup rollup) {
        Rollup.Snapshot before = rollup.take(sheet);
        settings.set(sheet, input.key(), input.value());
        audit.record("SETTING_CHANGED sheet=" + sheet + " by=" + user + " key=" + input.key() + " value="
                + input.value());
        rollup.record(sheet, user, before);
    }
}
