package net.officefloor.hq.app.watch;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithWatch {
    public void respond(@Val Person person, @Val WatchChange change, ObjectResponse<Watch> response) {
        response.send(new Watch(person.id(), change.by()));
    }
}
