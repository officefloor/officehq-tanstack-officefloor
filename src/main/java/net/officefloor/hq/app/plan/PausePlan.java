package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Pauses the plan on the sheet's today: its dates from then are skipped until it is resumed. Refuses a plan already
 *  paused. */
public class PausePlan {
    public void pause(@Val Sheet sheet, @Val Plan plan, PlanPauseRepository pauses) throws InvalidPlanException {
        if (pauses.isPaused(plan.getId())) {
            throw new InvalidPlanException("The plan is already paused.");
        }
        pauses.saveAndFlush(new PlanPause(plan.getId(), sheet.getToday(), null));
    }
}
