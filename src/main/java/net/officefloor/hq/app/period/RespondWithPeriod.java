package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriod {
    public void respond(@Val PeriodInput period, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.of(period.name(), period.from(), period.to()));
    }
}
