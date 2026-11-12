package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClosedPeriod {
    public void respond(@Val PeriodResponse period, ObjectResponse<PeriodResponse> response) {
        response.send(period);
    }
}
