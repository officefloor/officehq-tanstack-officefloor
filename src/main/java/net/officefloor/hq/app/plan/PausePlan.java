package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Pauses the plan from the sheet's today; a plan already paused is refused. Writes PLAN_PAUSED. */
public class PausePlan {
    public void pause(@PathVariable("sheetId") String sheetId, @RequestBody PlanPauseRequest request, @Val Plan plan,
            PlanRepository plans, PersonRepository people, Audit audit) throws InvalidPlanException {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (plan.paused()) {
            throw new InvalidPlanException("The plan is already paused");
        }
        Person person = people.find(sheet, plan.personId());
        plans.pause(sheet, plan.id());
        audit.record("PLAN_PAUSED sheet=" + sheet + " by=" + by + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start());
    }
}
