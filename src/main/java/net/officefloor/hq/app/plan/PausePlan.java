package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Pauses the plan from the sheet's today (PLAN_PAUSED); pausing a paused plan is refused. */
public class PausePlan {
    public void pause(@RequestParam(name = "by", required = false) String by, @Val Plan plan,
            @Val Person person, PlanRepository plans, SheetRepository sheets, Audit audit)
            throws InvalidPlanException {
        if (plan.paused()) throw new InvalidPlanException("The plan is already paused");
        plans.pause(plan.id(), sheets.today(plan.sheetId()));
        audit.record("PLAN_PAUSED sheet=" + plan.sheetId() + " by=" + (by == null ? "" : by)
                + " name=" + person.getName() + " start=" + plan.start());
    }
}
