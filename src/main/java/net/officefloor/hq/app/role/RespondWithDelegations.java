package net.officefloor.hq.app.role;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithDelegations {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, DelegationStore store,
            ObjectResponse<List<DelegationResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on)).stream().map(DelegationResponse::of).toList());
    }
}
