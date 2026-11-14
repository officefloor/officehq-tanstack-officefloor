package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Saves the user's locale; it is the user's own view of the sheet, so nothing is audited. */
public class SaveLocale {
    public void save(@Val Sheet sheet, @Val UserLocale locale, LocaleRepository repository) {
        repository.save(sheet.id(), locale.user(), locale.locale());
    }
}
