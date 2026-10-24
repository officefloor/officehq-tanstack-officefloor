package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSheet {

    public record SheetResponse(String id, String today) {
    }

    public void respond(@Val Sheet sheet, ObjectResponse<SheetResponse> response) {
        response.send(new SheetResponse(sheet.getId(), sheet.getToday().toString()));
    }
}
