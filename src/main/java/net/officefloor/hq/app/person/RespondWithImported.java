package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImported {
    public void respond(@Val ImportResponse imported, ObjectResponse<ImportResponse> response) {
        response.send(imported);
    }
}
