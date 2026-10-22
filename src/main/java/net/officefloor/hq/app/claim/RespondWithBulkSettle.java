package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkSettle {
    public void respond(@Val BulkSettleResponse result, ObjectResponse<BulkSettleResponse> response) {
        response.send(result);
    }
}
