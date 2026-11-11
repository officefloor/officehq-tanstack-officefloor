package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkRedate {
    public void respond(@Val BulkRedateResponse result, ObjectResponse<BulkRedateResponse> response) {
        response.send(result);
    }
}
