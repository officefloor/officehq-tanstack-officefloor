package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGenerated {
    public void respond(@Val GeneratedPeriods generated, ObjectResponse<GeneratedResponse> response) {
        response.send(new GeneratedResponse(generated.periods().size()));
    }
}
