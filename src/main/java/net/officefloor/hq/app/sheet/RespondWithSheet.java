package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithSheet {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, SheetStore store,
            ObjectResponse<SheetResponse> response) {
        String id = sheetId.trim();
        LocalDate on = History.date(asOf);
        store.markLoaded(id);
        response.send(new SheetResponse(id, store.today(id).toString(),
                on == null ? store.archived(id) : store.archivedBy(id, on), on == null ? null : on.toString()));
    }
}
