package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's holidays, earliest date first. */
public class RespondWithHolidays {
    public void respond(@PathVariable("sheetId") String sheetId, HolidayRepository repository,
            ObjectResponse<List<HolidayResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(HolidayResponse::from).toList());
    }
}
