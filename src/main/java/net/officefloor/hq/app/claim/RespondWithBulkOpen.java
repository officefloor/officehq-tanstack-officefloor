package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected claims were opened and how many refused. */
public class RespondWithBulkOpen {
    public void respond(@Val BulkOpenResult result, ObjectResponse<BulkOpenResult> response) {
        response.send(result);
    }
}
