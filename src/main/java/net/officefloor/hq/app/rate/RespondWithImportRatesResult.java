package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportRatesResult {
    public void respond(@Val ImportRatesResult result, ObjectResponse<ImportRatesResult> response) {
        response.send(result);
    }
}
