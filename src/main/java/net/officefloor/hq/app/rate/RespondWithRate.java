package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRate {
    public void respond(@Val Rate rate, RateRepository rates, ObjectResponse<RateResponse> response) {
        response.send(RateResponse.from(rate, 0, rates.older(rate.sheetId(), rate.grade(), rate.from())));
    }
}
