package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithBulkOpen {
    public void respond(@Val BulkOpened bulk, ObjectResponse<BulkOpenResponse> response) {
        response.send(new BulkOpenResponse(bulk.opened().size(), bulk.refused()));
    }
}
