package net.officefloor.hq.app.access;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class ApplySetting {
    public void apply(@Val SheetResponse sheet, @Val SettingInput input, SettingRepository repository,
            Out<Setting> setting) {
        setting.set(repository.set(sheet.id(), input.key(), input.value()));
    }
}
