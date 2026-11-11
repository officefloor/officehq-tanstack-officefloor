package net.officefloor.hq.app.plan;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Pauses a running plan from the sheet's today and audits it; pausing a paused plan is refused. */
public class PausePlan {
    public void pause(@Val Sheet sheet, @Val Plan plan, PlanRepository plans, PersonRepository people,
            HttpServletRequest request, Audit audit, Out<PlanPaused> done) throws InvalidPlanException {
        if (plan.paused()) throw new InvalidPlanException("The plan is already paused");
        plans.pause(plan.id(), sheet.today());
        var person = people.find(sheet.id(), plan.personId());
        audit.record("PLAN_PAUSED sheet=" + sheet.id() + " by=" + Audit.user(request) + " name="
                + (person == null ? "" : person.getName()) + " start=" + plan.start());
        done.set(new PlanPaused(plans.find(sheet.id(), plan.id())));
    }
}
