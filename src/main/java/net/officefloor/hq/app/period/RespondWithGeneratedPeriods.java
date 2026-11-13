package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGeneratedPeriods {
    public void respond(@Val GeneratedPeriods generated, ObjectResponse<GeneratePeriodsResult> response) {
        response.send(new GeneratePeriodsResult(generated.periods().size()));
    }
}
