package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;

/** Stores the user's locale. It writes no audit record. */
public class SaveLocale {
    public void save(@Val UserLocale locale, LocaleRepository locales) {
        locales.save(locale.user(), locale.locale());
    }
}
