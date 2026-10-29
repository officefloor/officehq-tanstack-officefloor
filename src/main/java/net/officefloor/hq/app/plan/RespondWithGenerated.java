package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGenerated {
    public void respond(@Val GenerateResponse generated, ObjectResponse<GenerateResponse> response) {
        response.send(generated);
    }
}
