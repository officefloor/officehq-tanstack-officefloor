package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class SaveSettings {
    public void save(@Val SettingsInput input, @Val Sheet sheet, SettingsRepository repository,
            Out<Settings> settingsOut) {
        Settings settings = new Settings(sheet.id(), input.clerkLimit());
        repository.save(settings);
        settingsOut.set(settings);
    }
}
