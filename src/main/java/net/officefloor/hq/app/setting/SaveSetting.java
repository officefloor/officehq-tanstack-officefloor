package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records the setting; a dated one from its from-date, else from the sheet's today. */
public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingInput setting, SettingStore store,
            SheetStore sheets) {
        String sheet = sheetId.trim();
        store.set(sheet, setting.key(), setting.value(),
                setting.from() != null ? setting.from() : sheets.today(sheet));
    }
}
