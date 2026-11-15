package net.officefloor.hq.app.user;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The user's locale (en unless chosen). */
public class LoadLocale {
    public void load(@PathVariable("user") String user, LocaleRepository locales,
            ObjectResponse<UserLocale> response) {
        String name = user.trim();
        response.send(new UserLocale(name, locales.localeOf(name)));
    }
}
