package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with where the person went: the other sheet and their id there. */
public class RespondWithTransferred {
    public void respond(@Val Transferred transferred, ObjectResponse<TransferResponse> response) {
        response.send(new TransferResponse(transferred.to(), transferred.person().getId()));
    }
}
