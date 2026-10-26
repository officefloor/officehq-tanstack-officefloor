package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatch {
    public void respond(@Val WatchRequest request, @Val Person person, ServerHttpConnection connection,
            ObjectResponse<WatchResponse> response) {
        response.send(new WatchResponse(person.id(), Audit.user(connection).trim()));
    }
}
