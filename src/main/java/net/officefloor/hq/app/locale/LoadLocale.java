package net.officefloor.hq.app.locale;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;

/** The signed-in user's locale, re-read from the store. */
public class LoadLocale {
    public void load(ServerHttpConnection connection, LocaleRepository repository, Out<UserLocale> locale)
            throws BadRequestException {
        String user = CurrentUser.require(connection);
        locale.set(new UserLocale(repository.locale(user)));
    }
}
