package net.officefloor.hq.app.importrates;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRatesSet {
    public void respond(@Val RatesSet set, ObjectResponse<RatesSet> response) {
        response.send(set);
    }
}
