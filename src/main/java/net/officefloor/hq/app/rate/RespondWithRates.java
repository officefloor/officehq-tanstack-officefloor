package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRates {
    public void respond(@Val Rates rates, ObjectResponse<Rates> response) {
        response.send(rates);
    }
}
