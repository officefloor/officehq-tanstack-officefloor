package net.officefloor.hq.app.user;

import net.officefloor.plugin.variable.Val;

/** Saves the user's locale. No audit line: it changes only how the screen shows figures. */
public class SaveLocale {
    public void save(@Val LocaleResponse chosen, Locales locales) {
        locales.set(chosen.user(), chosen.locale());
    }
}
