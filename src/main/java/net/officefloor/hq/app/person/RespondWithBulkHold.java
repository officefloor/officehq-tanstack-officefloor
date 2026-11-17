package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected people were held. */
public class RespondWithBulkHold {
    public void respond(@Val BulkHoldResult result, ObjectResponse<BulkHoldResult> response) {
        response.send(result);
    }
}
