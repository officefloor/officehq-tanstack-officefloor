package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Val;

/** Saves the user name's locale, replacing the one it had. Not audited: it changes no sheet. */
public class SaveLocale {
    public void save(@Val UserLocale input, UserLocaleRepository repository) {
        UserLocale saved = repository.findById(input.getUserName()).orElse(input);
        saved.setLocale(input.getLocale());
        repository.saveAndFlush(saved);
    }
}
