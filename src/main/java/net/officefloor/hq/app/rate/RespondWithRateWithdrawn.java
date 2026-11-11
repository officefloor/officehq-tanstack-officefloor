package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's rates once the rate is withdrawn. */
public class RespondWithRateWithdrawn {
    public void respond(@Val net.officefloor.hq.app.sheet.Sheet sheet, RateRepository repository,
            ObjectResponse<RatesResponse> response) {
        response.send(new RatesResponse(repository.findAll(sheet.id()).stream().map(RateResponse::from).toList()));
    }
}
