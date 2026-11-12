package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlanGenerate {
    public void respond(@Val PlanGenerateResult result, ObjectResponse<PlanGenerateResult> response) {
        response.send(result);
    }
}
