package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithEditedPerson {
    public void respond(@Val PersonResponse person, ObjectResponse<PersonResponse> response) {
        response.send(person);
    }
}
