package net.officefloor.hq.app.statement;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithStatements {
    public void respond(@PathVariable("sheetId") String sheetId, Statements statements,
            ObjectResponse<List<StatementResponse>> response) {
        response.send(statements.list(sheetId.trim()));
    }
}
