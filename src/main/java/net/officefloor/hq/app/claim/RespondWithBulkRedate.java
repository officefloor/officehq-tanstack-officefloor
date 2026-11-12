package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkRedate {
    public void respond(@Val BulkRedateResult result, ObjectResponse<BulkRedateResult> response) {
        response.send(result);
    }
}
