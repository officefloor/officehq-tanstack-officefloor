package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

/** Keep the signed-in user's locale. It changes only how the screen shows figures, so it writes no audit line. */
public class SaveLocale {
    public void save(@Val UserLocale locale, ServerHttpConnection connection, LocaleRepository repository)
            throws BadRequestException {
        repository.setLocale(CurrentUser.require(connection), locale.locale());
    }
}
