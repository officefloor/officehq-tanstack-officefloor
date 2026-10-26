package net.officefloor.hq.app.session;

import java.util.Set;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A locale choice: en or de. */
public class ValidateLocale {

    private static final Set<String> LOCALES = Set.of("en", "de");

    public void validate(@RequestBody LocaleRequest request, Out<LocaleRequest> out) throws BadRequestException {
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!LOCALES.contains(locale)) {
            throw new BadRequestException("A locale is en or de");
        }
        out.set(new LocaleRequest(locale));
    }
}
