package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The plan just saved. */
public class RespondWithPlan {
    public void respond(@Val Plans saved, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(saved.list().get(0)));
    }
}
