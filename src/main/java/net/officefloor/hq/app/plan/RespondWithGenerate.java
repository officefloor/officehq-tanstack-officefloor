package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGenerate {
    public void respond(@Val GenerateResponse result, ObjectResponse<GenerateResponse> response) {
        response.send(result);
    }
}
