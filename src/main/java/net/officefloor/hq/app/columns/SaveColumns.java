package net.officefloor.hq.app.columns;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

/** Keep the signed-in user's switched-off columns. It changes only what their screen shows, so it writes no audit line. */
public class SaveColumns {
    public void save(@Val UserColumns columns, ServerHttpConnection connection, ColumnsRepository repository)
            throws BadRequestException {
        repository.setHidden(CurrentUser.require(connection), columns.hidden());
    }
}
