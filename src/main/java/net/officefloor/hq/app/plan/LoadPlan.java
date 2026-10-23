package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Fails when the plan is not on the sheet. */
public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") String planId,
            PlanRepository repository, Out<Plan> plan) throws InvalidPlanException {
        Plan found = null;
        try {
            found = repository.find(sheetId.trim(), Long.parseLong(planId.trim()));
        } catch (NumberFormatException e) {
            // not an id: not found
        }
        if (found == null) {
            throw new InvalidPlanException("There is no plan " + planId + " on this sheet.");
        }
        plan.set(found);
    }
}
