package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;
import org.springframework.web.bind.annotation.PathVariable;

/** Saves each setting whose value changed and records SETTING_CHANGED for it once saved; an unchanged
 *  setting (or a floor or terms not given) writes nothing. The signed-in user arrives as X-User. */
public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input,
            @HttpHeaderParameter("X-User") String user, SettingRepository repository, Audit audit) {
        String id = sheetId.trim();
        String by = user == null ? "" : user.trim();
        change(id, by, Setting.CLERK_LIMIT, input.clerkLimit(), null, repository, audit);
        if (input.floor() != null) {
            change(id, by, Setting.FLOOR, input.floor(), Setting.DEFAULT_FLOOR, repository, audit);
        }
        if (input.terms() != null) {
            change(id, by, Setting.TERMS, input.terms(), Setting.DEFAULT_TERMS, repository, audit);
        }
    }

    private static void change(String id, String by, String key, String value, String fallback,
            SettingRepository repository, Audit audit) {
        Setting setting = repository.findBySheetIdAndKey(id, key).orElseGet(() -> new Setting(id, key));
        String was = setting.getValue() == null ? fallback : setting.getValue();
        if (was == null ? value == null : was.equals(value)) {
            return;
        }
        setting.setValue(value);
        repository.saveAndFlush(setting);
        audit.record("SETTING_CHANGED sheet=" + id + " by=" + by + " key=" + key + " value=" + (value == null ? "" : value));
    }
}
