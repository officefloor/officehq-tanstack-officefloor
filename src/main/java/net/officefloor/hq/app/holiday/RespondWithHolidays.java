package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithHolidays {
    public void respond(@PathVariable("sheetId") String sheetId, Holidays holidays,
            ObjectResponse<List<HolidayResponse>> response) {
        response.send(holidays.list(sheetId.trim()).stream().map(d -> new HolidayResponse(d.toString())).toList());
    }
}
