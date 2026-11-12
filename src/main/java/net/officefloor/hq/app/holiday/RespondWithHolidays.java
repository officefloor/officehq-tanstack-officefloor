package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class RespondWithHolidays {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, HolidayStore store,
            ObjectResponse<List<HolidayResponse>> response) {
        LocalDate on = History.date(asOf);
        response.send((on == null ? store.list(sheetId.trim()) : store.listAsOf(sheetId.trim(), on)).stream()
                .map(d -> new HolidayResponse(d.toString())).toList());
    }
}
