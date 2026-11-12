package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Keeps the user's chosen locale on the sheet. Not audited: it changes only how the screen shows figures. */
public class SaveLocale {
    public UserLocale save(@PathVariable("sheetId") String sheetId, @Val LocaleRequest request, LocaleStore locales) {
        locales.choose(sheetId.trim(), request.user(), request.locale());
        return new UserLocale(request.locale());
    }
}
