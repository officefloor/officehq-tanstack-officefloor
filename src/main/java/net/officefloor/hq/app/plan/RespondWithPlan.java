package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val Plan plan, SheetRepository sheets, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.of(plan, sheets.today(plan.sheetId())));
    }
}
