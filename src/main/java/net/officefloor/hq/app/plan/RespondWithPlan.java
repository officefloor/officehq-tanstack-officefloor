package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val PlanResponse saved, ObjectResponse<PlanResponse> response) {
        response.send(saved);
    }
}
