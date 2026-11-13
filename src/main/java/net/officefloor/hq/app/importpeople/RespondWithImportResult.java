package net.officefloor.hq.app.importpeople;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportResult {
    public void respond(@Val ImportResult result, ObjectResponse<ImportResult> response) {
        response.send(result);
    }
}
