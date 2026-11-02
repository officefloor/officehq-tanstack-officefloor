package net.officefloor.hq.app.plan;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's plan by id: refused as an invalid plan when the sheet has none such. */
public class LoadPlan {
    public void load(@PathVariable("sheet") String sheet, @PathVariable("plan") long plan, Plans plans,
            Out<Plan> loaded) throws InvalidPlanException {
        Plan found = plans.find(sheet, plan);
        if (found == null) {
            throw new InvalidPlanException("No such plan");
        }
        loaded.set(found);
    }
}
