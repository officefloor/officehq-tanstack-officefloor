package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveSettings {
    public void save(@PathVariable("sheetId") String sheetId, @Val SettingsInput input, SettingRepository repository,
            Out<Settings> settings) {
        if (input.setsClerkLimit()) {
            repository.setClerkLimit(sheetId.trim(), input.clerkLimit());
        }
        if (input.floor() != null) {
            repository.setFloor(sheetId.trim(), input.floor());
        }
        if (input.terms() != null) {
            repository.setTerms(sheetId.trim(), input.terms());
        }
        settings.set(repository.of(sheetId.trim()));
    }
}
