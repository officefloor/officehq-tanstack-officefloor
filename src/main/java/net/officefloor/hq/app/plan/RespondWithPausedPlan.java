package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPausedPlan {
    public void respond(@Val PlanPaused done, @Val Sheet sheet, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(done.plan(), sheet.today()));
    }
}
