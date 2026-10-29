package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithOpeningImport {
    public void respond(@Val OpeningImportResponse imported, ObjectResponse<OpeningImportResponse> response) {
        response.send(imported);
    }
}
