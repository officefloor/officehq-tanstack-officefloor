package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@RequestBody LocaleRequest request, Out<LocaleRequest> body) throws InvalidLocaleException {
        String locale = request.locale() == null ? "" : request.locale().trim();
        if (!UserLocales.isKnown(locale)) {
            throw new InvalidLocaleException(locale);
        }
        body.set(new LocaleRequest(locale));
    }
}
