package net.officefloor.hq.app.locale;

import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Keep the user's locale (no audit line: it changes only how figures are shown to them). */
public class SaveLocale {
    public void save(@Val LocaleInput input, LocaleRepository locales, Out<LocaleResponse> saved) {
        locales.save(input.user(), input.locale());
        saved.set(new LocaleResponse(input.user(), input.locale()));
    }
}
