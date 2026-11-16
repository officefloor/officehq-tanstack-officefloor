package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's holidays, by date. */
public class RespondWithHolidays {
    public void respond(@PathVariable("sheetId") String sheetId, HolidayRepository holidays,
            ObjectResponse<List<Holiday>> response) {
        response.send(holidays.list(sheetId.trim()).stream().map(d -> new Holiday(d.toString())).toList());
    }
}
