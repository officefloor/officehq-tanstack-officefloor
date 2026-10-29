package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRateImport {
    public void respond(@Val RateImportResponse imported, ObjectResponse<RateImportResponse> response) {
        response.send(imported);
    }
}
