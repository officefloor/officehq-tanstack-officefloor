package net.officefloor.hq.app.locale;

import net.officefloor.plugin.section.clazz.Parameter;
import net.officefloor.web.ObjectResponse;

public class RespondWithSavedLocale {
    public void respond(@Parameter UserLocale locale, ObjectResponse<UserLocale> response) {
        response.send(locale);
    }
}
