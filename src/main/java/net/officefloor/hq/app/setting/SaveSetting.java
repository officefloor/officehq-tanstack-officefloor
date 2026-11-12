package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingInput setting, SettingStore store) {
        store.set(sheetId.trim(), setting.key(), setting.value());
    }
}
