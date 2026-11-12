package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's rates as they are now, or as they stood at the end of the {@code asOf} date. */
public class RespondWithRates {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, RateStore store,
            ObjectResponse<List<RateResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store : store.asOf(on)).list(sheetId.trim()));
    }
}
