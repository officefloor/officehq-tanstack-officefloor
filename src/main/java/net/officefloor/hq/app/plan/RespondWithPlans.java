package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithPlans {
    public void respond(@Val Sheet sheet, PlanRepository repository, ObjectResponse<PlansResponse> response) {
        response.send(new PlansResponse(repository.findAll(sheet.id()).stream().map(PlanResponse::from).toList()));
    }
}
