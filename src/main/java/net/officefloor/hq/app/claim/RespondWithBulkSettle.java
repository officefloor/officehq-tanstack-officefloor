package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected claims were settled and how many refused. */
public class RespondWithBulkSettle {
    public void respond(@Val BulkSettleResult result, ObjectResponse<BulkSettleResult> response) {
        response.send(result);
    }
}
