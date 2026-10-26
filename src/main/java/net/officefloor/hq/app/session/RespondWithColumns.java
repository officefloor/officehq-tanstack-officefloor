package net.officefloor.hq.app.session;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithColumns {
    public void respond(@Val SheetResponse sheet, @Val UserResponse user, ColumnsRepository columns,
            ObjectResponse<ColumnsResponse> response) {
        response.send(new ColumnsResponse(user.name(), columns.hidden(sheet.id(), user.name())));
    }
}
