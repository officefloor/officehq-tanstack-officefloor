package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected claims were moved and how many refused. */
public class RespondWithBulkReassign {
    public void respond(@Val BulkReassignResult result, ObjectResponse<BulkReassignResult> response) {
        response.send(result);
    }
}
