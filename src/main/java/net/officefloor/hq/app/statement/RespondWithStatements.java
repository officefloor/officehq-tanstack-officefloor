package net.officefloor.hq.app.statement;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** For each person on the sheet with a statement: their statement count and their last statement. */
public class RespondWithStatements {
    public void respond(@PathVariable("sheetId") String sheetId, StatementRepository statements,
            ObjectResponse<List<StatementSummary>> response) {
        response.send(statements.summaries(sheetId.trim()));
    }
}
