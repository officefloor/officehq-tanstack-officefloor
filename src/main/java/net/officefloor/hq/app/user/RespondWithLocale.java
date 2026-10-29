package net.officefloor.hq.app.user;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithLocale {
    public void respond(@PathVariable("user") String user, Locales locales, ObjectResponse<LocaleResponse> response) {
        response.send(new LocaleResponse(locales.of(user.trim())));
    }
}
