package net.officefloor.hq.app.person;

import net.officefloor.hq.app.statement.StatementRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, StatementRepository statements, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, statements.findByPersonIdOrderByIdAsc(person.getId())));
    }
}
