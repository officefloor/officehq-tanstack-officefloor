package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Resumes the plan on the sheet's today: its dates from the day it was paused up to the day before are skipped, not
 *  postponed. Refuses a plan that is not paused. */
public class ResumePlan {
    public void resume(@Val Sheet sheet, @Val Plan plan, PlanPauseRepository pauses) throws InvalidPlanException {
        PlanPause open = pauses.findByPlanIdOrderByIdAsc(plan.getId()).stream().filter(PlanPause::isOpen).findFirst()
                .orElseThrow(() -> new InvalidPlanException("The plan is not paused."));
        open.resume(sheet.getToday());
        pauses.saveAndFlush(open);
    }
}
