package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Sets the setting's value on the sheet, replacing the one it had. */
public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingValues values, SettingRepository settings,
            Out<Setting> setting) {
        setting.set(settings.save(new Setting(sheetId.trim(), values.key(), values.value())));
    }
}
