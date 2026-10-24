package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGeneratedPlans {
    public void respond(@Val GeneratedPlans made, ObjectResponse<GeneratedPlans> response) {
        response.send(made);
    }
}
