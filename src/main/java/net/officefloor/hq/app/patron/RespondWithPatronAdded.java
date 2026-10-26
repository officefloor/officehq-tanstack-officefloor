package net.officefloor.hq.app.patron;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPatronAdded {
    public void respond(@Val Person person, @Val Patron patron, ObjectResponse<PatronResponse> response) {
        response.send(new PatronResponse(person.id(), patron.person().id()));
    }
}
