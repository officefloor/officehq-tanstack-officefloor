package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's plan named by the path; refused (400) when the sheet has none of that id. */
public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") long planId,
            PlanRepository plans, Out<Plan> plan) throws InvalidPlanException {
        Plan found = plans.find(sheetId, planId);
        if (found == null) {
            throw new InvalidPlanException("No such plan.");
        }
        plan.set(found);
    }
}
