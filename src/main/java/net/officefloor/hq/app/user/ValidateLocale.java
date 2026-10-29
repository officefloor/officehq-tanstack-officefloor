package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@PathVariable("user") String user, @RequestBody LocaleRequest request, Out<LocaleRequest> body)
            throws InvalidLocaleException {
        if (user.trim().isEmpty()) {
            throw new InvalidLocaleException("Sign in to choose a locale.");
        }
        if (request == null || request.locale() == null || !Locales.KNOWN.contains(request.locale().trim())) {
            throw new InvalidLocaleException("Choose a locale of " + String.join(", ", Locales.KNOWN) + ".");
        }
        body.set(new LocaleRequest(request.locale().trim()));
    }
}
