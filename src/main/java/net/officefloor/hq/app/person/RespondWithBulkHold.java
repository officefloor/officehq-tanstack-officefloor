package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkHold {
    public void respond(@Val BulkHoldResult result, ObjectResponse<BulkHoldResult> response) {
        response.send(result);
    }
}
