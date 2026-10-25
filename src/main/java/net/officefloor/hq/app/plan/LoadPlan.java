package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a plan of the sheet by id. */
public class LoadPlan {
    public void load(@Val Sheet sheet, @PathVariable("planId") String planId, PlanRepository repository,
            Out<Plan> planOut) throws PlanNotFoundException {
        Long id;
        try {
            id = Long.valueOf(planId.trim());
        } catch (NumberFormatException e) {
            throw new PlanNotFoundException(planId);
        }
        planOut.set(repository.findById(id).filter(p -> p.getSheetId().equals(sheet.getId()))
                .orElseThrow(() -> new PlanNotFoundException(planId)));
    }
}
