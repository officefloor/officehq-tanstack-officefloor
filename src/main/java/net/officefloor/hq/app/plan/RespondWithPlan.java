package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val Plan plan, Sheets sheets, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(plan, 0, sheets.today(plan.sheetId())));
    }
}
