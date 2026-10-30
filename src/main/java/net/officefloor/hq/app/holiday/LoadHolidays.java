package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's holidays, earliest first. */
public class LoadHolidays {
    public void service(@PathVariable("sheetId") String sheetId, Holidays holidays, ObjectResponse<List<String>> response) {
        response.send(holidays.list(sheetId));
    }
}
