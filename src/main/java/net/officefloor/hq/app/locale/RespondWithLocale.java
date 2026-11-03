package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithLocale {
    public void respond(@Val UserLocale locale, ObjectResponse<UserLocale> response) {
        response.send(locale);
    }
}
