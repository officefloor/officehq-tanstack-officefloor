package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            PlanStore store, Out<Plan> planOut) throws InvalidSheetException {
        Plan plan = null;
        try {
            plan = store.find(Long.parseLong(planId.trim()));
        } catch (NumberFormatException e) {
            // not an id: no such plan
        }
        if (plan == null || !plan.sheetId().equals(sheetId.trim())) {
            throw new InvalidSheetException("No such plan: " + planId);
        }
        planOut.set(plan);
    }
}
