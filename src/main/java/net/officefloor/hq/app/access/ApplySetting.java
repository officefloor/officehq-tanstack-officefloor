package net.officefloor.hq.app.access;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Sets the setting: a dated one records its value from the from-date given (the sheet's today when none was), keeping the others. */
public class ApplySetting {
    public void apply(@Val SheetResponse sheet, @Val SettingInput input, SettingRepository repository,
            SheetRepository sheets, Out<Setting> setting) {
        if (!Setting.isDated(input.key())) {
            setting.set(repository.set(sheet.id(), input.key(), input.value()));
            return;
        }
        repository.setFrom(sheet.id(), input.key(), input.from() != null ? input.from() : sheets.today(sheet.id()),
                input.value());
        setting.set(new Setting(sheet.id(), input.key(), input.value(), input.from()));
    }
}
