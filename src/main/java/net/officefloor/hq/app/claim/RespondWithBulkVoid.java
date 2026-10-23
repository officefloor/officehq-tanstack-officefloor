package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkVoid {
    public void respond(@Val BulkVoidResponse result, ObjectResponse<BulkVoidResponse> response) {
        response.send(result);
    }
}
