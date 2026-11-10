package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val Sheet sheet, SettingsRepository repository,
            ObjectResponse<SettingsResponse> response) {
        response.send(SettingsResponse.from(repository.find(sheet.id())));
    }
}
