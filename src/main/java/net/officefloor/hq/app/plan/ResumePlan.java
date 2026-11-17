package net.officefloor.hq.app.plan;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Resumes the paused plan from the sheet's today; a plan not paused is refused. Writes PLAN_RESUMED. */
public class ResumePlan {
    public void resume(@PathVariable("sheetId") String sheetId, @RequestBody PlanPauseRequest request, @Val Plan plan,
            PlanRepository plans, PersonRepository people, Audit audit) throws InvalidPlanException {
        String sheet = sheetId.trim();
        String by = request == null || request.by() == null ? "" : request.by().trim();
        if (!plan.paused()) {
            throw new InvalidPlanException("The plan is not paused");
        }
        Person person = people.find(sheet, plan.personId());
        plans.resume(sheet, plan.id());
        audit.record("PLAN_RESUMED sheet=" + sheet + " by=" + by + " name=" + (person == null ? "" : person.name())
                + " start=" + plan.start());
    }
}
