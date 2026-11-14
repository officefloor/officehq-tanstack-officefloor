package net.officefloor.hq.app.columns;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithColumns {
    public void respond(@Val UserColumns columns, ObjectResponse<UserColumns> response) {
        response.send(columns);
    }
}
