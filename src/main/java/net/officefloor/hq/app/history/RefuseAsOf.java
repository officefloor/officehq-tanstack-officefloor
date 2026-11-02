package net.officefloor.hq.app.history;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.server.http.ServerHttpConnection;

/** Refuse any change from a screen showing the sheet as of a date (400, nothing written). The first step of
 *  every endpoint that changes a sheet without {@link net.officefloor.hq.app.settings.RefuseViewer}. */
public class RefuseAsOf {
    public void refuse(ServerHttpConnection connection) throws BadRequestException {
        CurrentUser.require(connection);
        AsOfView.refuse(connection);
    }
}
