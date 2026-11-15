package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Save the user's locale, for themselves only; no audit record. */
public class SaveLocale {
    public void save(@PathVariable("user") String user, @Val LocaleRequest request, LocaleRepository locales,
            ObjectResponse<UserLocale> response) throws InvalidLocaleException {
        String name = user.trim();
        if (name.isEmpty()) {
            throw new InvalidLocaleException("Sign in to choose a locale.");
        }
        String locale = request.locale().trim();
        locales.set(name, locale);
        response.send(new UserLocale(name, locale));
    }
}
