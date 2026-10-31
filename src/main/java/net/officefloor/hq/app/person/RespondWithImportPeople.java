package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportPeople {
    public void respond(@Val ImportPeopleResult result, ObjectResponse<ImportPeopleResult> response) {
        response.send(result);
    }
}
