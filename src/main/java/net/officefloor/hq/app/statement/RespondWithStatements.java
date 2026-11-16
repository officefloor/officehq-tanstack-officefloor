package net.officefloor.hq.app.statement;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** For each person on the sheet with a statement: their statement count and their last statement; read as of a date
 *  (see AsOf), counting only the statements taken while the sheet's today was on or before it. */
public class RespondWithStatements {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(value = "asOf", required = false) String asOf, StatementRepository statements,
            ObjectResponse<List<StatementSummary>> response) {
        LocalDate date = AsOf.parse(asOf);
        response.send(date == null ? statements.summaries(sheetId.trim()) : statements.summaries(sheetId.trim(), date));
    }
}
