package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The period as stored now. */
public class RespondWithPeriod {
    public void respond(@Val Period period, PeriodRepository periods, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(periods.find(period.sheetId(), period.id())));
    }
}
