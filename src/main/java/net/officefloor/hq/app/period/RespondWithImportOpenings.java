package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportOpenings {
    public void respond(@Val ImportOpeningsResult result, ObjectResponse<ImportOpeningsResult> response) {
        response.send(result);
    }
}
