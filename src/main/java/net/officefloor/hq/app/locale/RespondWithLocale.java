package net.officefloor.hq.app.locale;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The user name's locale as the store holds it (en unless one is saved). */
public class RespondWithLocale {
    public void respond(@PathVariable("userName") String userName, UserLocaleRepository repository,
            ObjectResponse<LocaleResponse> response) {
        String user = userName == null ? "" : userName.trim();
        response.send(new LocaleResponse(user, repository.localeOf(user)));
    }
}
