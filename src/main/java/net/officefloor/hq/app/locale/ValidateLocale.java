package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@RequestBody LocaleRequest request, Out<LocaleRequest> body) throws InvalidSheetException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidSheetException("Sign in to choose a locale");
        }
        String locale = request.locale() == null ? "" : request.locale().trim();
        if (!LocaleStore.LOCALES.contains(locale)) {
            throw new InvalidSheetException("Choose a locale of " + String.join(", ", LocaleStore.LOCALES));
        }
        body.set(new LocaleRequest(request.user().trim(), locale));
    }
}
