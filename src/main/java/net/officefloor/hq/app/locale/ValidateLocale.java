package net.officefloor.hq.app.locale;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the locale form from the body; refuses a blank user name and a locale other than en or de. */
public class ValidateLocale {

    private static final Set<String> LOCALES = Set.of(UserLocale.EN, UserLocale.DE);

    public void validate(@PathVariable("userName") String userName, @RequestBody LocaleRequest request,
            Out<UserLocale> body) throws InvalidLocaleException {
        String user = userName == null ? "" : userName.trim();
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (user.isEmpty()) {
            throw new InvalidLocaleException("The user name must not be blank.");
        }
        if (!LOCALES.contains(locale)) {
            throw new InvalidLocaleException("The locale must be en or de.");
        }
        body.set(new UserLocale(user, locale));
    }
}
