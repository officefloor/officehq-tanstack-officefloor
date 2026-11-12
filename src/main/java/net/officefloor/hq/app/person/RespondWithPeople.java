package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.hq.app.statement.StatementStore;
import net.officefloor.hq.app.statement.StatementSummary;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPeople {
    public void respond(@PathVariable("sheetId") String sheetId, PersonStore store,
            StatementStore statements, PatronStore patrons, SheetStore sheets,
            ObjectResponse<List<PersonResponse>> response) {
        Map<Long, StatementSummary> stated = statements.summaries(sheetId.trim());
        Map<Long, List<Long>> named = patrons.all(sheetId.trim());
        LocalDate today = sheets.today(sheetId.trim());
        response.send(store.list(sheetId.trim()).stream()
                .map(p -> PersonResponse.from(p, today, stated.getOrDefault(p.getId(), StatementSummary.NONE),
                        named.getOrDefault(p.getId(), List.of())))
                .toList());
    }
}
