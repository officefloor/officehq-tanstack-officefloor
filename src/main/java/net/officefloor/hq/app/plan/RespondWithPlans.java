package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's plans as the screen reads them, in the order they were added. */
public class RespondWithPlans {
    public void respond(@Val Plans plans, ObjectResponse<List<PlanResponse>> response) {
        response.send(plans.list().stream().map(PlanResponse::from).toList());
    }
}
