package net.officefloor.hq.app.locale;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Responds with the signed-in user's locale on the sheet. */
public class RespondWithLocale {
    public void respond(@PathVariable("sheet") String sheet, @RequestParam("user") String user, Locales locales,
            ObjectResponse<UserLocale> response) {
        response.send(new UserLocale(locales.of(sheet, user)));
    }
}
