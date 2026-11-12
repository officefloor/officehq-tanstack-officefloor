package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's periods as they are now, or as they stood at the end of the {@code asOf} date. */
public class RespondWithPeriods {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PeriodStore store,
            ObjectResponse<List<PeriodResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send(on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on));
    }
}
