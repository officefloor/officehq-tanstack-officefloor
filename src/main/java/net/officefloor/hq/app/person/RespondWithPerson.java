package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person));
    }
}
