package net.officefloor.hq.app.setting;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSetting {
    public void respond(@Val Setting setting, ObjectResponse<Setting> response) {
        response.send(setting);
    }
}
