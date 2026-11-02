package net.officefloor.hq.app.session;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSession {
    public void respond(@Val Session session, ObjectResponse<Session> response) {
        response.send(session);
    }
}
