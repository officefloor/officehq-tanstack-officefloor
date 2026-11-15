package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListHolidays {
    public void list(@PathVariable("sheetId") String sheetId, HolidayRepository holidays,
            ObjectResponse<List<Holiday>> response) {
        response.send(holidays.list(sheetId.trim()));
    }
}
