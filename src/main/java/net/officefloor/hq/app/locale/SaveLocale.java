package net.officefloor.hq.app.locale;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.setting.InvalidSettingException;
import net.officefloor.plugin.variable.Val;

/** The signed-in user's locale is saved; it moves no figures and writes no audit line. */
public class SaveLocale {
    public void save(@Val LocaleInput input, LocaleRepository locales, HttpServletRequest request)
            throws InvalidSettingException {
        String user = Audit.user(request);
        if (user.isBlank()) throw new InvalidSettingException("Sign in to choose a locale");
        locales.save(user, input.locale());
    }
}
