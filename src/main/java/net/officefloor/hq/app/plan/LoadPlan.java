package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** The plan by its id on the sheet; refused when there is none. */
public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") long planId, Plans plans,
            Out<Plan> plan) throws InvalidSheetRequestException {
        Plan found = plans.find(sheetId, planId);
        if (found == null) throw new InvalidSheetRequestException("No such plan: " + planId);
        plan.set(found);
    }
}
