package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val Settings settings, ObjectResponse<SettingsResponse> response) {
        response.send(SettingsResponse.from(settings));
    }
}
