package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is written: a signed-in user and a locale offered. */
public class ValidateLocale {
    public void validate(@PathVariable("user") String user, @RequestBody LocaleRequest request, Out<LocaleInput> body)
            throws InvalidLocaleException {
        if (user == null || user.isBlank()) {
            throw new InvalidLocaleException("Sign in before choosing a locale.");
        }
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!LocaleRepository.LOCALES.contains(locale)) {
            throw new InvalidLocaleException("Choose a locale: " + String.join(" or ", LocaleRepository.LOCALES) + ".");
        }
        body.set(new LocaleInput(user.trim(), locale));
    }
}
