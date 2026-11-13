package net.officefloor.hq.app.period;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriods {
    public void respond(@Val List<Period> periods, ObjectResponse<List<PeriodResponse>> response) {
        response.send(periods.stream().map(PeriodResponse::from).toList());
    }
}
