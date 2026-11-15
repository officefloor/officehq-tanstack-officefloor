package net.officefloor.hq.app.holiday;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithHoliday {
    public void respond(@Val Holiday holiday, ObjectResponse<Holiday> response) {
        response.send(holiday);
    }
}
