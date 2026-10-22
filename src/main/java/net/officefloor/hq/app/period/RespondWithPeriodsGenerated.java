package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPeriodsGenerated {
    public void respond(@Val GeneratedPeriods generated, ObjectResponse<PeriodGenerateResponse> response) {
        response.send(new PeriodGenerateResponse(generated.periods().size()));
    }
}
