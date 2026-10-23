package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithLocale {
    public void respond(@Val LocaleResponse saved, ObjectResponse<LocaleResponse> response) {
        response.send(saved);
    }
}
