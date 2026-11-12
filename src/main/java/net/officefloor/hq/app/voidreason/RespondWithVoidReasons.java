package net.officefloor.hq.app.voidreason;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithVoidReasons {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, VoidReasonStore store,
            ObjectResponse<List<VoidReasonResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on)).stream()
                .map(VoidReasonResponse::new).toList());
    }
}
