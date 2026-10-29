package net.officefloor.hq.app.plan;

import java.util.List;
import java.util.Map;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, Plans plans, ObjectResponse<List<PlanResponse>> response) {
        String sheet = sheetId.trim();
        Map<Long, Integer> made = plans.madeCounts(sheet);
        response.send(plans.list(sheet).stream().map(p -> PlanResponse.from(p, made.getOrDefault(p.id(), 0))).toList());
    }
}
