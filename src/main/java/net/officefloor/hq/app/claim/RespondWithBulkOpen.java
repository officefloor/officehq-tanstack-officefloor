package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkOpen {
    public void respond(@Val BulkOpenResponse result, ObjectResponse<BulkOpenResponse> response) {
        response.send(result);
    }
}
