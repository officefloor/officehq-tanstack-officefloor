package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's holidays, earliest first. */
public class RespondWithHolidays {
    public void respond(@PathVariable("sheet") String sheet, Holidays holidays, ObjectResponse<List<String>> response) {
        response.send(holidays.list(sheet));
    }
}
