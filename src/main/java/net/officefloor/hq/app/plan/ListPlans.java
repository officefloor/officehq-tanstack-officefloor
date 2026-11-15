package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPlans {
    public void list(@PathVariable("sheetId") String sheetId, PlanRepository plans,
            ObjectResponse<List<Plan>> response) {
        response.send(plans.list(sheetId.trim()));
    }
}
