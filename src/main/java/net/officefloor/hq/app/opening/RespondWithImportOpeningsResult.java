package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportOpeningsResult {
    public void respond(@Val ImportOpeningsResult result, ObjectResponse<ImportOpeningsResult> response) {
        response.send(result);
    }
}
