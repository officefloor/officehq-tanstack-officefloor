package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveSetting {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val SettingInput input,
            Settings settings, Audit audit) {
        settings.set(sheet, input.key(), input.value());
        audit.record("SETTING_CHANGED sheet=" + sheet + " by=" + user + " key=" + input.key() + " value="
                + input.value());
    }
}
