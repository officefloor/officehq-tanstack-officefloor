package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatching {
    public void respond(@Val Person person, @Val Watching watching, ObjectResponse<WatchingResponse> response) {
        response.send(new WatchingResponse(person.id(), watching.value()));
    }
}
