package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Saves the user's locale. A display preference only: no audit line. */
public class SaveLocale {
    public void save(@PathVariable("user") String user, @Val LocaleRequest request, UserLocales locales,
            ObjectResponse<LocaleResponse> response) {
        locales.setLocale(user, request.locale());
        response.send(new LocaleResponse(locales.localeOf(user)));
    }
}
