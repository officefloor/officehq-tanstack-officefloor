package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's holidays as the screen reads them: their dates (yyyy-MM-dd), earliest first. */
public class RespondWithHolidays {
    public void respond(@Val Holidays holidays, ObjectResponse<List<String>> response) {
        response.send(holidays.list().stream().map((h) -> h.date().toString()).toList());
    }
}
