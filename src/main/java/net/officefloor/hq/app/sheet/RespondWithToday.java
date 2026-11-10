package net.officefloor.hq.app.sheet;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithToday {
    public void respond(@Val Sheet sheet, @Val LocalDate today, ObjectResponse<SheetResponse> response) {
        response.send(SheetResponse.from(new Sheet(sheet.id(), today, sheet.archived())));
    }
}
