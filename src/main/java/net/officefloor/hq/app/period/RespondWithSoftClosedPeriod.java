package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithSoftClosedPeriod {
    public void respond(@Val SoftClosedPeriod softClosed, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(softClosed.period()));
    }
}
