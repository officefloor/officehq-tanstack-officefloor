package net.officefloor.hq.app.plan;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every plan on the sheet, in the order they were added. */
public class RespondWithPlans {
    public void respond(@PathVariable("sheetId") String sheetId, PlanRepository repository,
            ObjectResponse<List<PlanResponse>> response) {
        response.send(repository.list(sheetId.trim()).stream().map(PlanResponse::from).toList());
    }
}
