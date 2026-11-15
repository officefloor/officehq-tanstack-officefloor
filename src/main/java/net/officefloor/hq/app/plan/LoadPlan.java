package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads the sheet's plan and its person; a plan not on the sheet is refused. */
public class LoadPlan {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("planId") long planId,
            PlanRepository plans, PersonRepository people, Out<Plan> planOut, Out<Person> personOut)
            throws InvalidPlanException {
        Plan plan = plans.find(planId);
        if (plan == null || !plan.sheetId().equals(sheetId.trim())) throw new InvalidPlanException("No such plan");
        Person person = people.find(plan.sheetId(), plan.personId());
        if (person == null) throw new InvalidPlanException("No such plan");
        planOut.set(plan);
        personOut.set(person);
    }
}
