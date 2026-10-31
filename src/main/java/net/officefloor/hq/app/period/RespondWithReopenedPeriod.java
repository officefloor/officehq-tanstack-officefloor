package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithReopenedPeriod {
    public void respond(@Val ReopenedPeriod reopened, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(reopened.period()));
    }
}
