package net.officefloor.hq.app.opening;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithOpeningImport {
    public void respond(@Val OpeningImportResult result, ObjectResponse<OpeningImportResult> response) {
        response.send(result);
    }
}
