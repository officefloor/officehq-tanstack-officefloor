package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriod {
    public void respond(@Val Period period, PeriodRepository repository, ObjectResponse<PeriodResponse> response) {
        response.send(PeriodResponse.from(period, repository.personTotals(period.sheetId()).get(period.name())));
    }
}
