package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The locale must be one the app knows. */
public class ValidateLocale {
    public void validate(@RequestBody LocaleRequest request, Out<LocaleRequest> body) throws InvalidLocaleException {
        if (request == null || request.locale() == null || !LocaleRepository.LOCALES.contains(request.locale().trim())) {
            throw new InvalidLocaleException("Choose a locale: en or de.");
        }
        body.set(request);
    }
}
