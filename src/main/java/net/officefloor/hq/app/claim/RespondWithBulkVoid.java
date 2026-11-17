package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected claims were voided and how many refused. */
public class RespondWithBulkVoid {
    public void respond(@Val BulkVoidResult result, ObjectResponse<BulkVoidResult> response) {
        response.send(result);
    }
}
