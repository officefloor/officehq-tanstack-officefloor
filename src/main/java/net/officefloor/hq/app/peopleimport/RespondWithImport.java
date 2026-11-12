package net.officefloor.hq.app.peopleimport;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImport {
    public void respond(@Val ImportResult result, ObjectResponse<ImportResult> response) {
        response.send(result);
    }
}
