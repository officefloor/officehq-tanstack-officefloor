package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's plans, in the order they were added. */
public class LoadPlans {
    public void service(@PathVariable("sheetId") String sheetId, Plans plans, ObjectResponse<List<PlanResponse>> response) {
        response.send(plans.list(sheetId).stream().map(PlanResponse::from).toList());
    }
}
