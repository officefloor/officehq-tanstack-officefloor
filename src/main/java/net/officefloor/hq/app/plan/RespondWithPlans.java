package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, PlanStore store,
            ObjectResponse<List<PlanResponse>> response) {
        response.send(store.list(sheetId.trim()).stream().map(PlanResponse::from).toList());
    }
}
