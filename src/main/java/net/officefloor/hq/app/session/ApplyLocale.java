package net.officefloor.hq.app.session;

import net.officefloor.plugin.variable.Val;

/** Save the user's locale (no audit line: it changes only how this user's screen shows figures). */
public class ApplyLocale {
    public void apply(@Val UserResponse user, @Val LocaleRequest request, LocaleRepository locales) {
        locales.setLocale(user.name(), request.locale());
    }
}
