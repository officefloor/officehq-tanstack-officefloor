package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class ApplyLocale {
    public void apply(@PathVariable("user") String user, @Val LocaleRequest request, Locales locales) {
        locales.set(user.trim(), request.locale());
    }
}
