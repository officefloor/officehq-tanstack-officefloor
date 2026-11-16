package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many of the selected people were retired. */
public class RespondWithBulkRetire {
    public void respond(@Val BulkRetireResult result, ObjectResponse<BulkRetireResult> response) {
        response.send(result);
    }
}
