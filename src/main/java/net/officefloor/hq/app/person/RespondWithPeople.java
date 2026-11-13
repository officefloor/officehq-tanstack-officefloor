package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeople {
    public void respond(@Val List<Person> people, ObjectResponse<List<PersonResponse>> response) {
        response.send(people.stream().map(PersonResponse::from).toList());
    }
}
