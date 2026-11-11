package net.officefloor.hq.app.locale;

import java.util.Set;
import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateLocale {

    private static final Set<String> LOCALES = Set.of("en", "de");

    public void validate(@RequestBody LocaleRequest request, Out<LocaleInput> body) throws InvalidSettingException {
        String locale = request == null || request.locale() == null ? "" : request.locale().trim();
        if (!LOCALES.contains(locale)) throw new InvalidSettingException("A locale is en or de");
        body.set(new LocaleInput(locale));
    }
}
