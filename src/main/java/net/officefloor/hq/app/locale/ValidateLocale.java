package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@RequestBody UserLocale request, Out<UserLocale> body) throws InvalidLocaleException {
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!Locales.ALL.contains(locale)) {
            throw new InvalidLocaleException("The locale must be en or de");
        }
        body.set(new UserLocale(locale));
    }
}
