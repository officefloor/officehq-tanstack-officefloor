package net.officefloor.hq.app.person;

import net.officefloor.web.ObjectResponse;
import net.officefloor.plugin.variable.Val;

/** Responds with where the person went. */
public class RespondWithTransferred {

    public record TransferredResponse(String name, String toSheet) {
    }

    public void respond(@Val PersonTransferred transferred, ObjectResponse<TransferredResponse> response) {
        response.send(new TransferredResponse(transferred.name(), transferred.toSheet()));
    }
}
