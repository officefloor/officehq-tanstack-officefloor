package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlan {
    public void respond(@Val Plan plan, SheetStore sheets, ObjectResponse<PlanResponse> response) {
        response.send(PlanResponse.from(plan, sheets.today(plan.sheetId())));
    }
}
