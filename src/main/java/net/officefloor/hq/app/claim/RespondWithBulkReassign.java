package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkReassign {
    public void respond(@Val BulkReassignResponse result, ObjectResponse<BulkReassignResponse> response) {
        response.send(result);
    }
}
