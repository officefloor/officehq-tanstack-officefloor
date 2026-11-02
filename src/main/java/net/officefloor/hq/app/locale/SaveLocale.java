package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Saves the user's locale. Not audited: it changes nothing on the sheet, only how the screen shows it to them. */
public class SaveLocale {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val UserLocale locale,
            Locales locales) {
        locales.set(sheet, user, locale.locale());
    }
}
