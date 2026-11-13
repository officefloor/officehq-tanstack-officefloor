package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSheet {
    public void respond(@Val Sheet sheet, ObjectResponse<SheetResponse> response) {
        response.send(SheetResponse.from(sheet));
    }
}
