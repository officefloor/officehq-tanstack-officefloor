package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSetting {
    public void respond(@Val Setting setting, ObjectResponse<SettingResponse> response) {
        response.send(SettingResponse.from(setting));
    }
}
