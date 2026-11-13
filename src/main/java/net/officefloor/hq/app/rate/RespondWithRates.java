package net.officefloor.hq.app.rate;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRates {
    public void respond(@Val List<Rate> rates, ObjectResponse<List<RateResponse>> response) {
        response.send(rates.stream().map(RateResponse::from).toList());
    }
}
