package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val Plan plan, @Val Sheet sheet, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(plan, sheet.today()));
    }
}
