package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithUnlockedPeriod {
    public void respond(@Val UnlockedPeriod unlocked, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(unlocked.period()));
    }
}
