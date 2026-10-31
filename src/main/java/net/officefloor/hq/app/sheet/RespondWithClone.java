package net.officefloor.hq.app.sheet;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The clone just made, as the screen sees a sheet. */
public class RespondWithClone {
    public void respond(@Val Clone clone, Sheets sheets, ObjectResponse<SheetResponse> response) {
        response.send(new SheetResponse(clone.to(), sheets.today(clone.to()).toString(), sheets.archived(clone.to())));
    }
}
