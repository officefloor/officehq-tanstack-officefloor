package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRates {
    public void respond(@Val Sheet sheet, RateRepository repository,
            ObjectResponse<RatesResponse> response) {
        response.send(new RatesResponse(
                repository.findAll(sheet.id()).stream().map(RateResponse::from).toList()));
    }
}
