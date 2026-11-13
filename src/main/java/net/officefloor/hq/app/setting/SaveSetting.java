package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records the setting; a dated one from its from-date, else from the sheet's today. A change to a policy
 *  setting is also the sheet's next policy version, from the same date. */
public class SaveSetting {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingInput setting, SettingStore store,
            SheetStore sheets) {
        String sheet = sheetId.trim();
        LocalDate from = setting.from() != null ? setting.from() : sheets.today(sheet);
        store.set(sheet, setting.key(), setting.value(), from);
        if (SettingStore.POLICY_KEYS.contains(setting.key())) {
            store.addPolicyVersion(sheet, setting.key(), setting.value(), from);
        }
    }
}
