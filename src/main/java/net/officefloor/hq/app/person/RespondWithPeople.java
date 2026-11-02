package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val People people, ObjectResponse<People> response) {
        response.send(people);
    }
}
