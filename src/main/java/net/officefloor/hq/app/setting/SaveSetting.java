package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records the setting's value on the sheet: a dated key from its from-date (the sheet's today when none was given),
 *  keeping the values recorded from other dates and replacing one recorded from the same date; an undated key
 *  replaces the value it had. */
public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingValues values, SettingRepository settings,
            SheetRepository sheets, Out<Setting> setting) {
        String sheet = sheetId.trim();
        setting.set(settings.save(new Setting(sheet, values.key(), values.value(),
                !values.dated() ? null : values.from() != null ? values.from() : sheets.today(sheet))));
    }
}
