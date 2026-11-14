package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The plan just saved. */
public class RespondWithPlan {
    public void respond(@Val Plans saved, SheetRepository sheets, ObjectResponse<PlanResponse> response) {
        Plan plan = saved.list().get(0);
        response.send(PlanResponse.from(plan, sheets.today(plan.sheetId())));
    }
}
