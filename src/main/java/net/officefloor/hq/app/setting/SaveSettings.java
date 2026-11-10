package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the settings being changed, leaving the others as they are. */
public class SaveSettings {
    public void save(@Val SettingsInput input, @Val Sheet sheet, SettingsRepository repository,
            Out<Settings> settingsOut) {
        Settings now = repository.find(sheet.id());
        Settings settings = new Settings(sheet.id(),
                input.clerkLimitChanged() ? input.clerkLimit() : now.clerkLimit(),
                input.floor() != null ? input.floor() : now.floor(),
                input.terms() != null ? input.terms() : now.terms());
        repository.save(settings);
        settingsOut.set(settings);
    }
}
