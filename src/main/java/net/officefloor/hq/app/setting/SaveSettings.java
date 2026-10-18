package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;
import org.springframework.web.bind.annotation.PathVariable;

/** Saves each setting whose value changed and records SETTING_CHANGED for it once saved; an unchanged
 *  setting writes nothing. The signed-in user arrives as X-User. */
public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input,
            @HttpHeaderParameter("X-User") String user, SettingRepository repository, Audit audit) {
        String id = sheetId.trim();
        Setting setting = repository.findBySheetIdAndKey(id, Setting.CLERK_LIMIT)
                .orElseGet(() -> new Setting(id, Setting.CLERK_LIMIT));
        String was = setting.getValue();
        if (was == null ? input.clerkLimit() == null : was.equals(input.clerkLimit())) {
            return;
        }
        setting.setValue(input.clerkLimit());
        repository.saveAndFlush(setting);
        audit.record("SETTING_CHANGED sheet=" + id + " by=" + (user == null ? "" : user.trim())
                + " key=" + Setting.CLERK_LIMIT + " value=" + (input.clerkLimit() == null ? "" : input.clerkLimit()));
    }
}
