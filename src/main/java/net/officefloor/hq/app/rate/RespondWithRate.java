package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithRate {
    public void respond(@Val Rate rate, ObjectResponse<Rate> response) {
        response.send(rate);
    }
}
