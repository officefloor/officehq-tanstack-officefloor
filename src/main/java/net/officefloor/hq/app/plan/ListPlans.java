package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListPlans {
    public void list(@PathVariable("sheetId") String sheetId, PlanRepository plans, SheetRepository sheets,
            ObjectResponse<List<ListedPlan>> response) {
        String id = sheetId.trim();
        LocalDate today = sheets.today(id);
        response.send(plans.list(id).stream().map((plan) -> ListedPlan.of(plan, today)).toList());
    }
}
