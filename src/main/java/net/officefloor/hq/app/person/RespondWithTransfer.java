package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with the person as they arrived on the other sheet. */
public class RespondWithTransfer {
    public void respond(@Val Transferred transferred, ObjectResponse<PersonResponse> response) {
        response.send(PersonResponse.from(transferred.person()));
    }
}
