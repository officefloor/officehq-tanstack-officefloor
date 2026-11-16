package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** Responds with how many claims the generate made. */
public class RespondWithGenerated {
    public void respond(@Val GenerateResult result, ObjectResponse<GenerateResult> response) {
        response.send(result);
    }
}
