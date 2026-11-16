package net.officefloor.hq.app.locale;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The user's locale as stored; en when they never chose one. */
public class RespondWithStoredLocale {
    public void respond(@PathVariable("user") String user, LocaleRepository locales, ObjectResponse<UserLocale> response) {
        String name = user.trim();
        response.send(new UserLocale(name, locales.locale(name)));
    }
}
