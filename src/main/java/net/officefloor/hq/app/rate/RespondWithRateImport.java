package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRateImport {
    public void respond(@Val RateImportResult result, ObjectResponse<RateImportResult> response) {
        response.send(result);
    }
}
