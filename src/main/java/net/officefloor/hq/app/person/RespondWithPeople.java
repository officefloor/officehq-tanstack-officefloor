package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.hq.app.statement.StatementSummary;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's people as they are now, or, as of a date, as they stood at the end of it (see {@link History})
 *  with the date standing in for the sheet's today. */
public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PersonStore store,
            StatementStore statements, PatronStore patrons, SheetStore sheets,
            ObjectResponse<List<PersonResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        Map<Long, StatementSummary> stated = on == null ? statements.summaries(sheet) : statements.summariesAsOf(sheet, on);
        Map<Long, List<Long>> named = on == null ? patrons.all(sheet) : patrons.allAsOf(sheet, on);
        LocalDate today = on == null ? sheets.today(sheet) : on;
        response.send((on == null ? store.list(sheet) : store.listAsOf(sheet, on)).stream()
                .map(p -> PersonResponse.from(p, today, stated.getOrDefault(p.getId(), StatementSummary.NONE),
                        named.getOrDefault(p.getId(), List.of())))
                .toList());
    }
}
