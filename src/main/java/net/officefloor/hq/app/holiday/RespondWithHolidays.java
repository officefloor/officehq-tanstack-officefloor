package net.officefloor.hq.app.holiday;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithHolidays {
    public void respond(@Val Sheet sheet, HolidayRepository repository, ObjectResponse<HolidaysResponse> response) {
        response.send(new HolidaysResponse(
                repository.findAll(sheet.id()).stream().map(HolidayResponse::from).toList()));
    }
}
