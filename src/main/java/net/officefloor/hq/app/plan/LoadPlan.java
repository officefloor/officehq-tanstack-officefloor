package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's plan of the path's id, as it is now; none is refused. Publishes it. */
public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            PlanRepository plans, Out<Plan> plan) throws InvalidPlanException {
        Plan found;
        try {
            found = plans.find(sheetId.trim(), Long.parseLong(planId.trim()));
        } catch (NumberFormatException e) {
            found = null;
        }
        if (found == null) {
            throw new InvalidPlanException("No plan " + planId);
        }
        plan.set(found);
    }
}
