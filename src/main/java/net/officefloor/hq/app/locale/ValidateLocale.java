package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the locale must be en or de, for a user who is named. */
public class ValidateLocale {
    public void validate(@PathVariable("user") String user, @RequestBody LocaleRequest request, Out<UserLocale> locale)
            throws InvalidLocaleException {
        String name = user == null ? "" : user.trim();
        String raw = request == null || request.locale() == null ? "" : request.locale().trim();
        if (name.isEmpty()) throw new InvalidLocaleException("No user");
        if (!LocaleRepository.LOCALES.contains(raw)) throw new InvalidLocaleException("Not a locale: " + raw);
        locale.set(new UserLocale(name, raw));
    }
}
