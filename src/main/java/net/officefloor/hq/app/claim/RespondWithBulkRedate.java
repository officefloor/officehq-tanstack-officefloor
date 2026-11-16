package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected claims were re-dated and how many refused. */
public class RespondWithBulkRedate {
    public void respond(@Val BulkRedateResult result, ObjectResponse<BulkRedateResult> response) {
        response.send(result);
    }
}
