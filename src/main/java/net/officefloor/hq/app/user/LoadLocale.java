package net.officefloor.hq.app.user;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The user's locale; {@code en} until they choose one. */
public class LoadLocale {
    public void service(@PathVariable("user") String user, UserLocales locales, ObjectResponse<LocaleResponse> response) {
        response.send(new LocaleResponse(locales.localeOf(user)));
    }
}
