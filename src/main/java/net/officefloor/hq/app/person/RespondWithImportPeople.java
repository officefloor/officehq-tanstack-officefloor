package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many imported people were added. */
public class RespondWithImportPeople {
    public void respond(@Val ImportPeopleResult result, ObjectResponse<ImportPeopleResult> response) {
        response.send(result);
    }
}
