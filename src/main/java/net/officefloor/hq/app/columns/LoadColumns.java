package net.officefloor.hq.app.columns;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;

/** The signed-in user's switched-off columns, re-read from the store. */
public class LoadColumns {
    public void load(ServerHttpConnection connection, ColumnsRepository repository, Out<UserColumns> columns)
            throws BadRequestException {
        String user = CurrentUser.require(connection);
        columns.set(new UserColumns(repository.hidden(user)));
    }
}
