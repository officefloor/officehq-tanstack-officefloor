package net.officefloor.hq.app.holiday;

import java.util.List;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's holidays, by date. */
public class RespondWithHolidays {
    public void respond(@Val Sheet sheet, HolidayRepository repository, ObjectResponse<List<HolidayResponse>> response) {
        response.send(repository.findBySheetIdOrderByDateAsc(sheet.getId()).stream().map(HolidayResponse::from).toList());
    }
}
