package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlans {
    public void respond(@Val Plans plans, ObjectResponse<Plans> response) {
        response.send(plans);
    }
}
