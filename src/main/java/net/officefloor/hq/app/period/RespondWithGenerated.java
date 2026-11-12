package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGenerated {
    public void respond(@Val GenerateResult result, ObjectResponse<GenerateResult> response) {
        response.send(result);
    }
}
