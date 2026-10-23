package net.officefloor.hq.app.locale;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The user's locale: 'en' until they choose one. */
public class RespondWithUserLocale {
    public void respond(@PathVariable("user") String user, LocaleRepository locales,
            ObjectResponse<LocaleResponse> response) {
        String name = user == null ? "" : user.trim();
        response.send(new LocaleResponse(name, locales.localeOf(name)));
    }
}
