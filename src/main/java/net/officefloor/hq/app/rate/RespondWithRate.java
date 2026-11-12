package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRate {
    public void respond(@Val RateInput rate, ObjectResponse<RateResponse> response) {
        response.send(RateResponse.of(rate.grade(), rate.pct(), rate.from(), rate.to()));
    }
}
