package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithLockedPeriod {
    public void respond(@Val LockedPeriod locked, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(locked.period()));
    }
}
