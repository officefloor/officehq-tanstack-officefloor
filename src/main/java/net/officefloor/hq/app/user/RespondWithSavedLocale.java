package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSavedLocale {
    public void respond(@Val LocaleResponse chosen, ObjectResponse<LocaleResponse> response) {
        response.send(chosen);
    }
}
