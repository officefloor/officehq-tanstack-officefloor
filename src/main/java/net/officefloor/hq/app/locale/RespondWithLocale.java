package net.officefloor.hq.app.locale;

import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The locale the user has chosen on the sheet; the default for no user or none chosen. */
public class RespondWithLocale {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "user", required = false) String user, LocaleStore locales,
            ObjectResponse<UserLocale> response) {
        response.send(new UserLocale(user == null || user.isBlank() ? LocaleStore.DEFAULT
                : locales.localeOf(sheetId.trim(), user.trim())));
    }
}
