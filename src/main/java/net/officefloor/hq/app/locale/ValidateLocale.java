package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {
    public void validate(@RequestBody LocaleRequest request, Out<UserLocale> locale) throws BadRequestException {
        String raw = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!LocaleRepository.LOCALES.contains(raw)) {
            throw new BadRequestException("locale must be one of " + String.join(", ", LocaleRepository.LOCALES));
        }
        locale.set(new UserLocale(raw));
    }
}
