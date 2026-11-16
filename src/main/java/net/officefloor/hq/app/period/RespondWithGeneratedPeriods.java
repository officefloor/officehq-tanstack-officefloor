package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGeneratedPeriods {
    public void respond(@Val GeneratedPeriods generated, ObjectResponse<GeneratedPeriods> response) {
        response.send(generated);
    }
}
