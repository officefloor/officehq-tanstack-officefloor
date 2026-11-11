package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the plan on the sheet; an unknown plan is refused. */
public class LoadPlan {
    public void load(@PathVariable("planId") String planId, @Val Sheet sheet, PlanRepository repository,
            Out<Plan> planOut) throws InvalidPlanException {
        Plan plan = null;
        try {
            plan = repository.find(sheet.id(), Long.parseLong(planId == null ? "" : planId.trim()));
        } catch (NumberFormatException e) {
            // not a plan id
        }
        if (plan == null) throw new InvalidPlanException("No such plan");
        planOut.set(plan);
    }
}
