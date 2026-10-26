package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportOpenings {
    public void respond(@Val ImportOpeningsResponse result, ObjectResponse<ImportOpeningsResponse> response) {
        response.send(result);
    }
}
