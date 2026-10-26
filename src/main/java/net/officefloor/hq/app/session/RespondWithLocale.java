package net.officefloor.hq.app.session;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithLocale {
    public void respond(@Val UserResponse user, LocaleRepository locales, ObjectResponse<LocaleResponse> response) {
        response.send(new LocaleResponse(user.name(), locales.locale(user.name())));
    }
}
