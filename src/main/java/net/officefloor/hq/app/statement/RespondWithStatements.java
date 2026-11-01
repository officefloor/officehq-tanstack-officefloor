package net.officefloor.hq.app.statement;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the statements taken on the sheet, per person. */
public class RespondWithStatements {
    public void respond(@PathVariable("sheet") String sheet, Statements statements,
            ObjectResponse<List<StatementSummary>> response) {
        response.send(statements.list(sheet));
    }
}
