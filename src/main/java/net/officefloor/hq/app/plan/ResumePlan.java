package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Resumes the paused plan on the sheet's today (PLAN_RESUMED); refused (400) when it is not paused. */
public class ResumePlan {
    public void resume(@Val Plan plan, @RequestParam("user") String user, PlanRepository plans, SheetRepository sheets,
            PersonRepository people, Audit audit, Out<Plans> saved) throws InvalidPlanException {
        if (!plan.paused()) {
            throw new InvalidPlanException("The plan is not paused.");
        }
        plans.resume(plan, sheets.today(plan.sheetId()));
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_RESUMED sheet=" + plan.sheetId() + " by=" + user + " name=" + person.name() + " start="
                + plan.start());
        saved.set(new Plans(java.util.List.of(plans.find(plan.sheetId(), plan.id()))));
    }
}
