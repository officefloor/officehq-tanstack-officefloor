package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGeneratedPeriods {
    public void respond(@Val GeneratePeriodsResponse result, ObjectResponse<GeneratePeriodsResponse> response) {
        response.send(result);
    }
}
