package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, PersonRepository repository, ObjectResponse<PersonResponse> response) {
        person.setClashing(repository.clashing(person));
        response.send(PersonResponse.from(person));
    }
}
