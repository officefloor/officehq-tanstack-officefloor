package net.officefloor.hq.app.statement;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithStatement {
    public void respond(@Val Person person, Statements statements, ObjectResponse<StatementResponse> response) {
        response.send(statements.find(person.sheetId(), person.id()));
    }
}
