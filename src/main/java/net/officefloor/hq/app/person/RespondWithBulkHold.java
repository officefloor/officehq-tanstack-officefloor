package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkHold {
    public void respond(@Val BulkHoldResponse result, ObjectResponse<BulkHoldResponse> response) {
        response.send(result);
    }
}
