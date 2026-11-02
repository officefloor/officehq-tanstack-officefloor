package net.officefloor.hq.app.settings;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSettings {
    public void respond(@Val Settings settings, ObjectResponse<Settings> response) {
        response.send(settings);
    }
}
