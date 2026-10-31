package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatch {
    public void respond(@Val WatchChange change, @Val Person person, ObjectResponse<WatchResponse> response) {
        response.send(new WatchResponse(person.getId(), change.watching()));
    }
}
