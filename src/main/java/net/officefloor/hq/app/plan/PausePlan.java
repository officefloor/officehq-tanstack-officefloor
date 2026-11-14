package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Pauses the plan from the sheet's today (PLAN_PAUSED); refused (400) when it is already paused. */
public class PausePlan {
    public void pause(@Val Plan plan, @RequestParam("user") String user, PlanRepository plans, SheetRepository sheets,
            PersonRepository people, Audit audit, Out<Plans> saved) throws InvalidPlanException {
        if (plan.paused()) {
            throw new InvalidPlanException("The plan is already paused.");
        }
        plans.pause(plan, sheets.today(plan.sheetId()));
        Person person = people.find(plan.sheetId(), plan.personId());
        audit.record("PLAN_PAUSED sheet=" + plan.sheetId() + " by=" + user + " name=" + person.name() + " start="
                + plan.start());
        saved.set(new Plans(java.util.List.of(plans.find(plan.sheetId(), plan.id()))));
    }
}
