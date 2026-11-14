package net.officefloor.hq.app.locale;

import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Refuses (400) a locale other than en or de. */
public class ValidateLocaleRequest {

    private static final Set<String> LOCALES = Set.of("en", "de");

    public void validate(@RequestParam("user") String user, @RequestBody LocaleRequest request,
            Out<UserLocale> locale) throws InvalidLocaleException {
        String chosen = request.locale() == null ? "" : request.locale().trim();
        if (!LOCALES.contains(chosen)) {
            throw new InvalidLocaleException("The locale must be en or de.");
        }
        locale.set(new UserLocale(user, chosen));
    }
}
