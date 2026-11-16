package net.officefloor.hq.app.statement;

import net.officefloor.web.ObjectResponse;
import net.officefloor.plugin.variable.Val;

public class RespondWithStatementCancelled {
    public void respond(@Val Statement statement, ObjectResponse<Statement> response) {
        response.send(statement);
    }
}
