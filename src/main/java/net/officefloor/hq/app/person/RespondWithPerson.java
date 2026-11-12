package net.officefloor.hq.app.person;

import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPerson {
    public void respond(@Val Person person, StatementStore statements, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(person, statements.summary(person.getSheetId(), person.getId())));
    }
}
