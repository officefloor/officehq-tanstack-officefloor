package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val Plan plan, Plans plans, Sheets sheets, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(plan, plans.made(plan.id()).size(), sheets.today(plan.sheetId()), plans.pauses(plan.id())));
    }
}
