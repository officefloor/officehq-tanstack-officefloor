package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithImportRates {
    public void respond(@Val ImportRatesResponse result, ObjectResponse<ImportRatesResponse> response) {
        response.send(result);
    }
}
